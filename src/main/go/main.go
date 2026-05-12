package main

import (
    "context"
    "io"
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

    tmp, err := os.CreateTemp("", "trivy-input-*")
    if err != nil {
        http.Error(w, "failed to create temp file", http.StatusInternalServerError)
        return
    }
    defer os.Remove(tmp.Name())

    if _, err := io.Copy(tmp, r.Body); err != nil {
        http.Error(w, "failed to read body", http.StatusBadRequest)
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 30*time.Second)
    defer cancel()

    cmd := exec.CommandContext(
        ctx,
        "trivy",
        "fs",
        tmp.Name(),
        "--format", "json",
        "--quiet",
    )

    w.Header().Set("Content-Type", "application/json")
    cmd.Stdout = w
    cmd.Stderr = os.Stderr

    if err := cmd.Run(); err != nil {
        // Trivy uses non-zero exit codes for findings; do not treat as server error
        log.Printf("trivy exited with error: %v", err)
    }
}

func main() {
    http.HandleFunc("/scan", scanHandler)

    log.Printf("listening on %s", address)
    log.Fatal(http.ListenAndServe(address, nil))
}