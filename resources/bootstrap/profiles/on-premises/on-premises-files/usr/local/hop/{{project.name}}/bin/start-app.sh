#!/usr/bin/env bash

set -eu -o pipefail

# Stop any containers still running. Don't wait for them to finish :-)
docker/docker-compose.sh down --timeout 0

# Make sure to check that we are not trying to use any environment
# vars in docker-compose.yml that are not set.
./docker/docker-env-vars.sh
docker/docker-compose.sh up --detach --force-recreate --renew-anon-volumes

# And show the logs
docker/docker-compose.sh logs --follow --timestamps --no-color
