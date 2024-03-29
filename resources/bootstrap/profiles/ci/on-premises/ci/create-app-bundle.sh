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
CURR_DIR="$(pwd)"
export COMPOSE_FILE="{{project.docker-compose.to-deploy}}"

function build_docker_compose_yml() {
    DOCKER_COMPOSE_VERSION="$(docker-compose --version | sed 's/.* version \([^ ,]*\).*/\1/g')"
    case "${DOCKER_COMPOSE_VERSION}" in
    1.*)
        DOCKER_COMPOSE_ARGS=('config')
        ;;
    *)
        DOCKER_COMPOSE_ARGS=('config' '--no-normalize')
        ;;
    esac

    COMPOSE_MERGED_FILE="$(mktemp -p "${DOCKER_COMPOSE_TMPDIR}" docker-compose-merged.XXXXXXXX)"
    docker-compose "${DOCKER_COMPOSE_ARGS[@]}" >"${COMPOSE_MERGED_FILE}"

    # yq script to create an intermediate YAML file with just the
    # entries in the various docker-compose files that include an
    # environment variable as their **value**.
    # shellcheck disable=SC2016
    GATHER_ALL_ENV_VARS='
        . as $item
        ireduce (
            {};
            . * (
                    $item
                    |
                    [
                        {"key": "x-hop-env-filler", "value": "x-hop-filler"}
                    ]
                    +
                    [
                        ..
                        |
                        select(. == "*$*")
                        |
                        {"key": path | join("."), "value": .}
                    ]
                    |
                    from_entries
                    |
                    to_props
                    |
                    from_props
                )
        )'

    # yq script to load the merged docker-compose file created above,
    # and overwrite (merge) the entries that we want to use
    # environment variables (computed in the above script). Once
    # merged, set to null any environment variable (of any service)
    # that doesn't use another environment variable as its
    # **value**.
    #
    # All of this is to basically undo the variable interpolation
    # "docker-compose config" does by default. We can't use
    # "--non-interpolate" in that case because we sometimes use
    # environment variables for entries that expect "sizes". And
    # docker-compose v2.x is very strict when checking the values of
    # those entries, and fails with an error. So we do the
    # "docker-compose config" with interpolation, and then undo it
    # here.
    printf -v UNDO_COMPOSE_INTERPOLATION '
        load("%s") *d? .
        |
        .services[].environment?
        |=
        map_values(
            . | select(. | test("\$[{a-zA-Z_]") | not) | . = null
        )' "${COMPOSE_MERGED_FILE}"

    # Build a bash array with each individual compose file as an entry.
    readarray -d ':' -t COMPOSE_FILES_ARRAY < <(echo -n "${COMPOSE_FILE}")

    yq eval-all "${GATHER_ALL_ENV_VARS}" "${COMPOSE_FILES_ARRAY[@]}" |
        yq eval "${UNDO_COMPOSE_INTERPOLATION}" |
        # Do final cleanup on the merged file:
        #  - Replace the "latest" image tag with the commit tag.
        #  - Remove empty environment sections produced by the "un-interpolate" process.
        #  - "reset" volume paths that are supposed to be relative, back to it's relative
        #    path (docker-compose expands them to full paths as part of the merge process)
        #  - Remove the top-level "name" key added by docker-compose v2.x, if present.
        #  - Remove the top-level "version" key added by docker-compose v2.x, if present.
        sed -e "s|${DOCKER_IMAGE_REPOSITORY}:latest|${DOCKER_IMAGE_REPOSITORY}:${TAG}|g" \
            -e '/^[[:space:]]*environment: \[\]/d' \
            -e "s|${CURR_DIR}|.|g" \
            -e '/^name: /d' \
            -e '/^version: /d'
}

build_docker_compose_yml >"${DOCKER_COMPOSE_TMPDIR}/docker-compose.yml"

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
