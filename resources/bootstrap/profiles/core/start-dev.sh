#!/usr/bin/env bash

set -eu -o pipefail

# These two values are used to build the aws-vault profile name.
# The aws-vault profile used is "${CUSTOMER}/${PROJECT}-dev-env"
CUSTOMER="{{project.customer-name}}"
PROJECT="{{project.name}}"

################################################################
#                                                              #
#  No user configurable parts below. Modify at your own risk!  #
#                                                              #
#  If it breaks, you get to keep the pieces :-)                #
#                                                              #
################################################################

# Tell docker-compose what compose files to use to run the different commands below
# Order of the files IS IMPORTANT, as later files overwrite values from previous ones.
# See https://docs.docker.com/compose/reference/envvars/#compose_file
export COMPOSE_FILE="{{project.profiles.docker-compose.dev}}"

# Get the project name used by docker-compose. We need it to clean up
# only containers related to this project, but not others.
compose_project="$(ci/compose-project-name.sh)"

# Stop any containers still running. Don't wait for them to finish :-)
docker-compose down -t0

# Clean up any left overs from previous runs, to make sure we start
# with a clean environment (stale Docker containers, stale Clojure
# code, etc). Make sure we only clean up containers (and their linked
# volumes) that belong to the project, and leave other containers
# alone.
CONTAINERS=$(docker ps -aq --filter "status=exited" --filter "status=dead" \
    --filter "status=created" --filter "name=^${compose_project}_")
if [[ -n "${CONTAINERS}" ]]; then
    echo "Removing exited/dead containers..."
    #shellcheck disable=SC2086
    docker rm -v ${CONTAINERS}
fi
VOLUMES=$(docker volume ls -qf dangling=true)

if [[ -n "${VOLUMES}" ]]; then
    echo "Removing dangling volumes..."
    #shellcheck disable=SC2086
    docker volume rm ${VOLUMES}
fi
docker-compose run --no-deps app lein clean

# Finally launch the application itself, using the development
# environment user credentials. Also make sure to check that we are
# not trying to use any environment vars in docker-compose.yml that
# are not set.
# And set the TEST_AWS_* env variables to the same values as the
# AWS_* ones. When running tests locally in the dev environment
# we should use the dev env credentials (not so in the CI pipelines).
#
#shellcheck disable=SC2016
local_dev_creds=$(aws-vault exec --no-session magnet/local-dev -- \
    bash -c 'echo "${AWS_ACCESS_KEY_ID},${AWS_SECRET_ACCESS_KEY},${AWS_DEFAULT_REGION}"')
TEST_AWS_ACCESS_KEY_ID=$(echo "${local_dev_creds}" | cut --delimiter=',' --fields=1)
TEST_AWS_SECRET_ACCESS_KEY=$(echo "${local_dev_creds}" | cut --delimiter=',' --fields=2)
TEST_AWS_DEFAULT_REGION=$(echo "${local_dev_creds}" | cut --delimiter=',' --fields=3)
aws-vault exec "${CUSTOMER}/${PROJECT}-dev-env" --duration 12h -- env \
    TEST_AWS_ACCESS_KEY_ID="${TEST_AWS_ACCESS_KEY_ID}" \
    TEST_AWS_DEFAULT_REGION="${TEST_AWS_DEFAULT_REGION}" \
    TEST_AWS_SECRET_ACCESS_KEY="${TEST_AWS_SECRET_ACCESS_KEY}" \
    bash -c \
    'set -eu;
     ./ci/docker-env-vars.sh;
     docker-compose up --build --detach --force-recreate --renew-anon-volumes'

# And show the logs
docker-compose logs -ft