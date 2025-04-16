#!/usr/bin/env bash

set -eu -o pipefail

# These values are used to build the aws-vault profile names.
PROFILE_PREFIX="{{project.profiles.aws.aws-vault.profile-prefix}}"
LOCAL_USER_NAME="{{project.profiles.aws.credentials.local-dev-user.name}}"
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
export COMPOSE_FILE="{{project.docker-compose.to-develop}}"

# Get the project name used by docker-compose. We need it to clean up
# only containers related to this project, but not others.
compose_project="$(docker/compose-project-name.sh)"

# Stop any containers still running. Don't wait for them to finish :-)
docker/docker-compose.sh down --timeout 0

# Clean up any left overs from previous runs, to make sure we start
# with a clean environment (stale Docker containers, stale Clojure
# code, etc). Make sure we only clean up containers (and their linked
# volumes) that belong to the project, and leave other containers
# alone.
CONTAINERS=$(docker ps --all --quiet --filter "status=exited" \
    --filter "status=dead" --filter "status=created" \
    --filter "name=^${compose_project}_")
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
# container. If it doesn't, the first time we launch the app
# container, the directory is going to be created owned by root
# (because it is a bind mount point). And that is going to prevent
# lein from downloading any dependencies!
mkdir -p ~/.m2/

# NOTE: we are overwriting the entrypoint here because otherwise "lein
# clean" would run with the "hop" user permissions due to the
# "docker/run-as-user.sh" entrypoint script of the "Dockerfile", and
# it's a problem. This is a problem when, for various $REASONS, new
# files or folders are created inside the "app/target" folder with the
# "root" user. When that happens it prevents the cleanup of
# "app/target" folder because some files/folders are owned by the
# "root" user and the "hop" user has no permissions to delete those
# files. Moreover, "lein clean" fails silently in those cases and its
# exit code is not propagated by Docker, so the script continues
# executing the next commands like nothing happened. So, to prevent
# that we overwrite the entrypoint, which skips the "hop" user
# configuration and thus the "lein clean" command is run by the "root"
# user which will ensure we always succeed on cleaning the
# "app/target" folder.
docker/docker-compose.sh run --no-deps --entrypoint /bin/bash --rm app lein clean

# Finally launch the application itself, using the development
# environment user credentials. Also make sure to check that we are
# not trying to use any environment vars in docker-compose.yml that
# are not set.
# And set the TEST_AWS_* env variables to the same values as the
# AWS_* ones. When running tests locally in the dev environment
# we should use the dev env credentials (not so in the CI pipelines).
#
#shellcheck disable=SC2016
local_dev_creds=$(aws-vault exec --no-session "${PROFILE_PREFIX}/${LOCAL_USER_NAME}" -- \
    bash -c 'echo "${AWS_ACCESS_KEY_ID},${AWS_SECRET_ACCESS_KEY},${AWS_DEFAULT_REGION}"')
TEST_AWS_ACCESS_KEY_ID=$(echo "${local_dev_creds}" | cut -d ',' -f 1)
TEST_AWS_SECRET_ACCESS_KEY=$(echo "${local_dev_creds}" | cut -d ',' -f 2)
TEST_AWS_DEFAULT_REGION=$(echo "${local_dev_creds}" | cut -d ',' -f 3)
aws-vault exec "${PROFILE_PREFIX}/${PROJECT}-dev-env" --duration 12h -- env \
    TEST_AWS_ACCESS_KEY_ID="${TEST_AWS_ACCESS_KEY_ID}" \
    TEST_AWS_DEFAULT_REGION="${TEST_AWS_DEFAULT_REGION}" \
    TEST_AWS_SECRET_ACCESS_KEY="${TEST_AWS_SECRET_ACCESS_KEY}" \
    bash -c \
    'set -eu;
     docker/docker-env-vars.sh;
     docker/docker-compose.sh up --build --detach --force-recreate --renew-anon-volumes'

# And show the logs
docker/docker-compose.sh logs --follow --timestamps
