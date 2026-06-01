package main

import (
	"bytes"
	"context"
	"errors"
	"io"
	"io/fs"
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

var (
	trivyDBPath    = getenv("TRIVY_DB_PATH", "/opt/trivy-db")
	trivyCachePath = getenv("TRIVY_CACHE_PATH", "/tmp/trivy-cache")

	execCommand = exec.CommandContext
)

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func copyDir(src, dst string) error {
	return filepath.WalkDir(src, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}

		rel, err := filepath.Rel(src, path)
		if err != nil {
			return err
		}

		target := filepath.Join(dst, rel)

		if d.IsDir() {
			return os.MkdirAll(target, 0o755)
		}

		in, err := os.Open(path)
		if err != nil {
			return err
		}

		out, err := os.Create(target)
		if err != nil {
			_ = in.Close()
			return err
		}

		_, err = io.Copy(out, in)

		cerr1 := in.Close()
		cerr2 := out.Close()

		if err != nil {
			return err
		}
		if cerr1 != nil {
			return cerr1
		}
		if cerr2 != nil {
			return cerr2
		}

		return nil
	})
}

func ensureCacheInitialized() error {
	metadata := filepath.Join(trivyCachePath, "db", "metadata.json")

	if _, err := os.Stat(metadata); err == nil {
		return nil
	}

	log.Printf("Initializing cache at %s", trivyCachePath)

	if err := os.RemoveAll(trivyCachePath); err != nil {
		log.Printf("cleanup failed: %v", err)
	}

	return copyDir(trivyDBPath, trivyCachePath)
}

func scanHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	if r.Body == nil {
		http.Error(w, "empty body", http.StatusBadRequest)
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize)
	defer func(Body io.ReadCloser) { _ = Body.Close() }(r.Body)

	ctx, cancel := context.WithTimeout(r.Context(), 30*time.Second)
	defer cancel()

	cmd := execCommand(
		ctx,
		"trivy",
		"fs",
		"-",
		"--format", "json",
		"--quiet",
		"--scanners", "vuln",
		"--disable-telemetry",
		"--offline-scan",
		"--skip-db-update",
	)

	cmd.Stdin = r.Body
	cmd.Stderr = os.Stderr

	var out bytes.Buffer
	cmd.Stdout = &out

	if err := cmd.Run(); err != nil {
		if errors.Is(err, context.DeadlineExceeded) || ctx.Err() == context.DeadlineExceeded {
			http.Error(w, "scan timeout", http.StatusGatewayTimeout)
			return
		}

		log.Printf("scan failed: %v", err)
		http.Error(w, "scan failed", http.StatusBadGateway)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(out.Bytes())
}

func main() {
	if err := ensureCacheInitialized(); err != nil {
		log.Printf("failed to initialize cache: %v", err)
		os.Exit(1)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/scan", scanHandler)

	srv := &http.Server{
		Addr:         address,
		Handler:      mux,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 60 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	log.Printf("listening on %s", address)
	log.Fatal(srv.ListenAndServe())
}
