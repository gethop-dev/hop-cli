#!/usr/bin/env bash
#shellcheck disable=SC2034

set -eu -o pipefail

# Tag to be used when creating the Docker image.
TAG="$(git rev-parse --short=10 HEAD)"

# Branch on which the current image is being build.
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# The application will be deployed only when commits belong to this branch
DEPLOYMENT_BRANCH="{{project.profiles.ci.continuous-deployment.git.deployment-branch}}"

# Docker repository in which the application image will be pushed
DOCKER_IMAGE_REPOSITORY="{{#lambdas.resolve-choices}}project.docker.registry.?.app-repository-url{{/lambdas.resolve-choices}}"
