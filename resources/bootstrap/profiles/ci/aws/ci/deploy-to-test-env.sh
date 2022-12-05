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
    yq --yaml-output '
                      # Take all the "environment" sections from all the
                      # services. Do not fail if the environment section does not
                      # exist in one or more services.
                      .services[].environment?
                      # Update their value using the expression below.
                      |=
                      (
                          # If the environment section did not exist or was empty,
                          if . == null
                          then
                              # Set it to an empty array
                              []
                          else
                              # Otherwise process each entry individually as a
                              # hash-map of {key: value}, where "key" is the
                              # variable name, and "value" is its associated
                              # value.
                              to_entries |
                              map(
                                  # If the entry value contains a dollar sign,
                                  # we want to keep the variable name ("key") and its
                                  # value ("value"), because we are building the value
                                  # from other environment variables.
                                  if (.value != null) and (.value | contains("${"))
                                  then
                                      # Build a string by join both the name and
                                      # the value with an equal sign
                                      [.key, .value] | join("=")
                                  else
                                      # If it did not contain a dollar sign,
                                      # simply keep the name of the variable
                                      # (the "key" in the hash-map)
                                     .key
                                  end
                              )
                          end
                      )' \
        >"${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"
sed -i \
    -e "s|${DOCKER_IMAGE_REPOSITORY}:latest|${DOCKER_IMAGE_REPOSITORY}:${TAG}|g" \
    -e "s|${CURR_DIR}|/var/app/current|g" \
    "${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"
cp -ra --parents "${SOURCE_BUNDLE_FILES[@]}" "${DOCKER_COMPOSE_TMPDIR}"
pushd "${DOCKER_COMPOSE_TMPDIR}"
zip -r "${KEY}" "${SOURCE_BUNDLE_FILES[@]}" docker-compose.yml
cp "${KEY}" "${CURR_DIR}/local.zip"
aws s3 cp "${KEY}" "s3://${EB_S3_BUCKET}/${KEY}"
popd

TS=$(date +%s)
VERSION_LABEL="${TS}-${TAG}"

# Create a new version
aws elasticbeanstalk create-application-version \
    --application-name "${APPLICATION_NAME}" \
    --version-label="${VERSION_LABEL}" \
    --source-bundle "S3Bucket=${EB_S3_BUCKET},S3Key=${KEY}" \
    --no-auto-create-application

# Update test environment
aws elasticbeanstalk update-environment \
    --application-name "${APPLICATION_NAME}" \
    --environment-name "${TEST_ENV_NAME}" \
    --version-label "${VERSION_LABEL}"
