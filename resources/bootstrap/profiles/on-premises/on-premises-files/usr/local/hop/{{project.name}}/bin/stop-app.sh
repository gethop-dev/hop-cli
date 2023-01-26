#!/usr/bin/env bash

set -eu -o pipefail

## Stop any containers still running. Don't wait for them to finish :-)
docker-compose down -t0
