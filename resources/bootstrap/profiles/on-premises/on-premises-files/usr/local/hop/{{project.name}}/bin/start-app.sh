#!/usr/bin/env bash

set -eu -o pipefail

# Stop any containers still running. Don't wait for them to finish :-)
docker-compose down -t0

# Make sure to check that we are not trying to use any environment
# vars in docker-compose.yml that are not set.
docker-compose up -d --force-recreate

# And show the logs
docker-compose logs -ft --no-color
