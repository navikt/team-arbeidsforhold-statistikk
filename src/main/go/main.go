package main

import (
	"context"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"time"
)

const (
	maxUploadSize = 2 << 20 // 2 MiB
	address       = ":8080"
)

func copyDir(src, dst string) error {
	return filepath.Walk(src, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		rel, err := filepath.Rel(src, path)
		if err != nil {
			return err
		}

		target := filepath.Join(dst, rel)

		if info.IsDir() {
			return os.MkdirAll(target, 0o755)
		}

		in, err := os.Open(path)
		if err != nil {
			return err
		}
		defer func() { _ = in.Close() }()

		out, err := os.Create(target)
		if err != nil {
			return err
		}

		defer func() {
			if e := out.Close(); e != nil {
				_, _ = fmt.Fprintf(os.Stderr, "close error: %v\n", e)
			}
		}()


		if _, err := io.Copy(out, in); err != nil {
			return err
		}

		return out.Sync()
	})
}

func ensureCacheInitialized() error {
	src := "/opt/trivy-db"
	dst := "/tmp/trivy-cache"

	if _, err := os.Stat(filepath.Join(dst, "db", "metadata.json")); err == nil {
		return nil
	}

	fmt.Println("Initializing Trivy cache in /tmp...")

	_ = os.RemoveAll(dst)

	return copyDir(src, dst)
}

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
	if err := ensureCacheInitialized(); err != nil {
		_, _ = fmt.Fprintf(os.Stderr, "failed to initialize cache: %v\n", err)
		os.Exit(1)
	}

	http.HandleFunc("/scan", scanHandler)

	log.Printf("listening on %s", address)
	log.Fatal(http.ListenAndServe(address, nil))
}
