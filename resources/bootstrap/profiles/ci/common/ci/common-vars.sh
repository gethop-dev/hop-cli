#!/usr/bin/env bash
#shellcheck disable=SC2034

set -eu -o pipefail

# Tag to be used when creating the Docker image.
TAG="$(git rev-parse --short HEAD)"

# Branch on which the current image is being build.
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

DOCKER_IMAGE_REPOSITORY="{{#lambdas.resolve-choices}}project.docker.registry.?.app-repository-url{{/lambdas.resolve-choices}}"
