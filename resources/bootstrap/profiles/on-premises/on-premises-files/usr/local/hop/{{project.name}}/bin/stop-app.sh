#!/usr/bin/env bash

set -eu -o pipefail

## Stop any containers still running. Don't wait for them to finish :-)
docker/docker-compose.sh down -t0
