#!/usr/bin/env bash
#shellcheck disable=SC2091

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
# shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

aws ecr get-login-password --region "${AWS_DEFAULT_REGION}" |
    sed "s/$(printf '\r')\$//" |
    docker login --username AWS --password-stdin "${ECR_REGISTRY}"
