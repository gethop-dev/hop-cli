#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
# shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

# Skip updating if branch is not main
if [[ "${BRANCH:=}" != "${DEPLOYMENT_BRANCH}" ]]; then
    exit 0
fi

# Use a temporary directory for all intermediate working files, and
# cleanup it up no matter the outcome.
DOCKER_COMPOSE_TMPDIR=$(realpath "$(mktemp -d ./tmp.XXXXXXXX)")
#shellcheck disable=SC2064
trap "rm -rf ${DOCKER_COMPOSE_TMPDIR}" INT TERM EXIT

# Build docker-compose.yml for the new application version, from the
# individual docker-compose.* files.
export COMPOSE_FILE="{{project.docker-compose.to-deploy}}"

# Build a bash array with each individual compose file as an entry.
readarray -d ':' -t COMPOSE_FILES_ARRAY < <(echo -n "${COMPOSE_FILE}")

env PERSISTENT_DATA_DIR="/non/existent/path" \
    "${SCRIPT_DIR}/merge-docker-compose-files.clj" \
    "${COMPOSE_FILES_ARRAY[@]}" \
    >"${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"

# Replace the "latest" image tag with the commit tag.
sed -i -e "s|${DOCKER_IMAGE_REPOSITORY}:latest|${DOCKER_IMAGE_REPOSITORY}:${TAG}|g" \
    "${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"

cp -ra --parents "${EB_SOURCE_BUNDLE_FILES[@]}" "${DOCKER_COMPOSE_TMPDIR}"
(
    cd "${DOCKER_COMPOSE_TMPDIR}" &&
        zip -r "${EB_SOURCE_BUNDLE_NAME}" "${EB_SOURCE_BUNDLE_FILES[@]}" docker-compose.yml &&
        aws s3 cp "${EB_SOURCE_BUNDLE_NAME}" "s3://${EB_S3_BUCKET}/${EB_SOURCE_BUNDLE_NAME}"
)

TIMESTAMP=$(date +%s)
VERSION_LABEL="${TIMESTAMP}-${TAG}"

# Create a new version
aws elasticbeanstalk create-application-version \
    --application-name "${EB_APPLICATION_NAME}" \
    --version-label="${VERSION_LABEL}" \
    --source-bundle "S3Bucket=${EB_S3_BUCKET},S3Key=${EB_SOURCE_BUNDLE_NAME}" \
    --no-auto-create-application

# Update test environment
aws elasticbeanstalk update-environment \
    --application-name "${EB_APPLICATION_NAME}" \
    --environment-name "${EB_TEST_ENV_NAME}" \
    --version-label "${VERSION_LABEL}"
