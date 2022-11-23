#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
#shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

# Skip building if branch is not master
if [[ "${BRANCH}" != "master" ]]; then
    exit 0
fi

docker tag "${DOCKER_APP_PROD_IMAGE_NAME}:latest" "${DOCKER_IMAGE_REGISTRY}/${DOCKER_APP_PROD_IMAGE_NAME}:${TAG}"
docker tag "${DOCKER_APP_PROD_IMAGE_NAME}:latest" "${DOCKER_IMAGE_REGISTRY}/${DOCKER_APP_PROD_IMAGE_NAME}:latest"

docker push "${DOCKER_IMAGE_REGISTRY}/${DOCKER_APP_PROD_IMAGE_NAME}:${TAG}"
docker push "${DOCKER_IMAGE_REGISTRY}/${DOCKER_APP_PROD_IMAGE_NAME}:latest"
