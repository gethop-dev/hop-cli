#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
# shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

# Skip updating if branch is not main
if [[ "${BRANCH:=}" != "main" ]]; then
    exit 0
fi

# Upload new version definition
DOCKER_COMPOSE_TMPDIR=$(realpath "$(mktemp -d ./tmp.XXXXXXXX)")
CURR_DIR="$(pwd)"
#shellcheck disable=SC2064
trap "rm -rf ${DOCKER_COMPOSE_TMPDIR}" INT TERM EXIT
export COMPOSE_FILE="{{project.docker-compose.to-deploy}}"
docker-compose config |
    yq --yaml-output '.services[].environment? |= (if . == null then [] else keys end)' \
        >"${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"
sed -i \
    -e "s|${DOCKER_IMAGE_REPOSITORY}:latest|${DOCKER_IMAGE_REPOSITORY}:${TAG}|g" \
    -e "s|${CURR_DIR}|/var/app/current|g" \
    "${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"
cp -ra --parents "${SOURCE_BUNDLE_FILES[@]}" "${DOCKER_COMPOSE_TMPDIR}"
pushd "${DOCKER_COMPOSE_TMPDIR}"
zip -r "${KEY}" "${SOURCE_BUNDLE_FILES[@]}" docker-compose.yml
cp "${KEY}" "${CURR_DIR}/local.zip"
aws s3 cp "${KEY}" "s3://${S3_BUCKET}/${KEY}"
popd

TS=$(date +%s)
VERSION_LABEL="${TS}-${TAG}"

# Create a new version
aws elasticbeanstalk create-application-version \
    --application-name "${APPLICATION_NAME}" \
    --version-label="${VERSION_LABEL}" \
    --source-bundle "S3Bucket=${S3_BUCKET},S3Key=${KEY}" \
    --no-auto-create-application

# Update test environment
aws elasticbeanstalk update-environment \
    --application-name "${APPLICATION_NAME}" \
    --environment-name "${TEST_ENV_NAME}" \
    --version-label "${VERSION_LABEL}"
