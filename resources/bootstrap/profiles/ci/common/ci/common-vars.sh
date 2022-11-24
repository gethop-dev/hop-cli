#!/usr/bin/env bash
#shellcheck disable=SC2034

set -eu -o pipefail

# Tag to be used when creating the Docker image.
TAG="$(git rev-parse --short HEAD)"

# Branch on which the current image is being build.
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

DOCKER_IMAGE_REGISTRY="{{#lambdas.resolve-choices}}project.docker.registry.?.app-repository-url{{/lambdas.resolve-choices}}"

# Docker image names for the builds
DOCKER_APP_PROD_IMAGE_NAME="{{project.name}}"

{{#cloud-provider.aws.enabled}}
SCRIPT_DIR=$(dirname "$(realpath "$0")")
source "${SCRIPT_DIR}/aws/common-vars.sh"
{{/cloud-provider.aws.enabled}}
