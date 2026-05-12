package main

import (
    "context"
    "errors"
    "log"
    "net/http"
    "os"
    "os/exec"
    "time"
)

const (
    maxUploadSize = 2 << 20 // 2 MiB
    address       = ":8080"
)

func scanHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
    defer r.Body.Close()


    ctx, cancel := context.WithTimeout(r.Context(), 30*time.Second)
    defer cancel()

    cmd := exec.CommandContext(
        ctx,
        "trivy",
        "fs",
        "-", // read from stdin
        "--format", "json",
        "--quiet",
        "--scanners", "vuln",
        "--disable-telemetry",
        "--offline-scan",
        "--skip-db-update",
    )

    cmd.Stdin = r.Body

    w.Header().Set("Content-Type", "application/json")
    cmd.Stdout = w
    cmd.Stderr = os.Stderr

    if err := cmd.Run(); err != nil {
        if errors.Is(ctx.Err(), context.DeadlineExceeded) {
            http.Error(w, "scan timeout", http.StatusGatewayTimeout)
            return
        }
        http.Error(w, "Trivy run failed", http.StatusBadGateway)
        return
    }
}

func main() {
    http.HandleFunc("/scan", scanHandler)

    log.Printf("listening on %s", address)
    log.Fatal(http.ListenAndServe(address, nil))
}