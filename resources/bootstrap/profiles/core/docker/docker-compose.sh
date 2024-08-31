#!/usr/bin/env bash

set -eu -o pipefail

if docker compose version >/dev/null 2>/dev/null; then
    docker compose "$@"
else
    docker-compose "$@"
fi
