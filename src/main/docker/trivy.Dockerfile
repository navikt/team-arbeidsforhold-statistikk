# syntax=docker.io/docker/dockerfile:1.7-labs
FROM golang:1.26-alpine AS builder
WORKDIR /src
COPY src/main/go/main.go .
COPY src/main/go/go.mod .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
    go build -trimpath -ldflags="-s -w" -o wrapper

FROM ghcr.io/aquasecurity/trivy:0.70.0 AS trivy-db
ENV TRIVY_CACHE_DIR=/root/.cache/trivy
RUN trivy image --download-db-only
RUN trivy image --download-java-db-only
RUN mkdir /empty && trivy fs /empty || true
RUN ls -R /root/.cache/trivy

FROM ghcr.io/aquasecurity/trivy:0.70.0
ENV TRIVY_CACHE_DIR=/root/.cache/trivy
COPY --from=trivy-db /root/.cache/trivy /root/.cache/trivy
COPY --from=builder /src/wrapper /wrapper

ENTRYPOINT ["/wrapper"]
