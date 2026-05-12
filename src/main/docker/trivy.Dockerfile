# syntax=docker.io/docker/dockerfile:1.7-labs
FROM golang:1.26-alpine AS builder
WORKDIR /src
COPY src/main/go/main.go .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
    go build -trimpath -ldflags="-s -w" -o wrapper

FROM aquasecurity/trivy:latest

COPY --from=builder /src/wrapper /wrapper

USER 65532
ENTRYPOINT ["/wrapper"]
