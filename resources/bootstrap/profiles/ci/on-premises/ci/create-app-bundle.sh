#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
# shellcheck disable=SC1090,SC1091
. "${SCRIPT_DIR}/common-vars.sh"

# Skip bundle creation if branch is not the deployment branch
if [[ "${BRANCH:=}" != "${DEPLOYMENT_BRANCH}" ]]; then
    exit 0
fi

# Set umask to remove the write permission to group and others.
# Certain CI enviroments have a 0000 umask, and the checkout and any
# created files during the CI job have full permissions for
# everybody. Which is a big no, no if those files end up in the
# deployment bundle!
umask 0022

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

cp -ra --parents "${SOURCE_BUNDLE_FILES[@]}" "${DOCKER_COMPOSE_TMPDIR}"

TIMESTAMP=$(date +%s)
VERSION_LABEL="${TIMESTAMP}-${TAG}"
TAR_FILE="${APPLICATION_NAME}-full-app-${VERSION_LABEL}.tar"

echo "Creating the final application bundle..."
(
    cd "${DOCKER_COMPOSE_TMPDIR}" &&
        find . -exec chmod go-w {} ";" &&
        tar -cf "${TAR_FILE}" "${SOURCE_BUNDLE_FILES[@]}" docker-compose.yml
)

mv "${DOCKER_COMPOSE_TMPDIR}/${TAR_FILE}" .

sha256sum "${TAR_FILE}" >"${TAR_FILE}".sha256sum

cat <<EOF
Done!

The files needed to deploy on premise are ready. These are the two
files that are needed:

    ${TAR_FILE}
    ${TAR_FILE}.sha256sum

EOF
