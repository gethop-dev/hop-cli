#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
#shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

# Skip building if branch is not master
if [[ "${BRANCH}" != "${DEPLOYMENT_BRANCH}" ]]; then
    exit 0
fi

docker tag "app:latest" "${DOCKER_IMAGE_REPOSITORY}:${TAG}"
docker tag "app:latest" "${DOCKER_IMAGE_REPOSITORY}:latest"

docker push "${DOCKER_IMAGE_REPOSITORY}:${TAG}"
docker push "${DOCKER_IMAGE_REPOSITORY}:latest"
