#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
# shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

# Skip updating if branch is not main
if [[ "${BRANCH:=}" != "${DEPLOYMENT_BRANCH}" ]]; then
    exit 0
fi

# Upload new version definition
DOCKER_COMPOSE_TMPDIR=$(realpath "$(mktemp -d ./tmp.XXXXXXXX)")
CURR_DIR="$(pwd)"
#shellcheck disable=SC2064
trap "rm -rf ${DOCKER_COMPOSE_TMPDIR}" INT TERM EXIT
export COMPOSE_FILE="{{project.docker-compose.to-deploy}}"
#shellcheck disable=SC2016
docker-compose config --no-interpolate |
    yq '.services[].environment?
        |=
        (
            with(select(. == null);
                . = []
            )
            |
            . as $all_env_vars
            |
            (
                $all_env_vars
                |
                to_entries
                |
                map(
                    select(  (.value != null) and (.value | contains("$")))
                    |
                    .key + "=" + .value
                )
                +
                (
                    $all_env_vars
                    |
                    to_entries
                    |
                    map(
                        select((.value != null) and (.value | contains("$") | not))
                        |
                        .key
                    )
                )
            )
            |
            sort
        )' \
            >"${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"
sed -i \
    -e "s|${DOCKER_IMAGE_REPOSITORY}:latest|${DOCKER_IMAGE_REPOSITORY}:${TAG}|g" \
    -e "s|${CURR_DIR}|/var/app/current|g" \
    "${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"
cp -ra --parents "${EB_SOURCE_BUNDLE_FILES[@]}" "${DOCKER_COMPOSE_TMPDIR}"
pushd "${DOCKER_COMPOSE_TMPDIR}"
zip -r "${EB_SOURCE_BUNDLE_NAME}" "${EB_SOURCE_BUNDLE_FILES[@]}" docker-compose.yml
cp "${EB_SOURCE_BUNDLE_NAME}" "${CURR_DIR}/local.zip"
aws s3 cp "${EB_SOURCE_BUNDLE_NAME}" "s3://${EB_S3_BUCKET}/${EB_SOURCE_BUNDLE_NAME}"
popd

TS=$(date +%s)
VERSION_LABEL="${TS}-${TAG}"

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
