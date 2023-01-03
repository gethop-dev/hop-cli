#!/usr/bin/env bash

set -eu -o pipefail

# Tell docker-compose what compose files to use to run the different commands below
# Order of the files IS IMPORTANT, as later files overwrite values from previous ones.
# See https://docs.docker.com/compose/reference/envvars/#compose_file
export COMPOSE_FILE="{{project.docker-compose.to-develop}}"

# Get the project name used by docker-compose. We need it to clean up
# only containers related to this project, but not others.
compose_project="$(docker/compose-project-name.sh)"

# Stop any containers still running. Don't wait for them to finish :-)
docker-compose down --timeout 0

# Clean up any left overs from previous runs, to make sure we start
# with a clean environment (stale Docker containers, stale Clojure
# code, etc). Make sure we only clean up containers (and their linked
# volumes) that belong to the project, and leave other containers
# alone.
CONTAINERS=$(docker ps --all --quiet --filter "status=exited" --filter "status=dead" \
    --filter "status=created" --filter "name=^${compose_project}_")
if [[ -n "${CONTAINERS}" ]]; then
    echo "Removing exited/dead containers..."
    #shellcheck disable=SC2086
    docker rm -v ${CONTAINERS}
fi
VOLUMES=$(docker volume ls --quiet \
    --filter "label=com.docker.compose.project=${compose_project}")

if [[ -n "${VOLUMES}" ]]; then
    echo "Removing existing project volumes..."
    #shellcheck disable=SC2086
    docker volume rm ${VOLUMES}
fi


# Make sure the Maven cache directory exists before launching the app
# container. If it doesn't, the first time we launch the app container
# is going to be created owned by root (because it is a bind mount
# point). And that is going to prevent downloading any dependencies!
mkdir -p ~/.m2/
docker-compose run --no-deps --rm app lein clean

# Make sure we are not trying to use any environment vars in
# docker-compose.yml that are not set.
./docker/docker-env-vars.sh

# Finally launch the application itself
docker-compose up --build --detach --force-recreate --renew-anon-volumes

# And show the logs
docker-compose logs --follow --timestamps
