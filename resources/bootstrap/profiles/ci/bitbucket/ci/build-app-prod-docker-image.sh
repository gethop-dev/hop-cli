#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
#shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

docker build --tag "${DOCKER_APP_PROD_IMAGE_NAME}" --target prod .
