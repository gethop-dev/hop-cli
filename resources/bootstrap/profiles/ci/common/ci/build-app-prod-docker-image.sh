#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
#shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

docker build --tag "app" --target prod --build-arg "GIT_TAG=${TAG}" .
