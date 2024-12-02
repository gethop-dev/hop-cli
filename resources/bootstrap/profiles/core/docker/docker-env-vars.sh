#!/usr/bin/env bash

set -eu -o pipefail

SCRIPT_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"

function get_missing_env_vars() {
    DOCKER_COMPOSE_VERSION="$("${SCRIPT_DIR}/docker-compose.sh" version --short)"

    case "${DOCKER_COMPOSE_VERSION}" in
    1.*)
        DOCKER_COMPOSE_ARGS=('config')
        ;;
    *)
        DOCKER_COMPOSE_ARGS=('config' '--no-normalize')
        ;;
    esac

    "${SCRIPT_DIR}/docker-compose.sh" "${DOCKER_COMPOSE_ARGS[@]}" |
        docker run --rm --interactive mikefarah/yq '.services[].environment?
            |=
            (
                with(select(. == null);
                    . = []
                )
                |
                to_entries
                |
                map(
                    select((.value == null))
                    |
                    .key + "=" + ">>HOP_ENV_VAR_MISSING_VALUE<<"
                )
            )' |
        awk -F'=' '/>>HOP_ENV_VAR_MISSING_VALUE<</ {print $1}'
}

missing_vars=$(get_missing_env_vars)

if [[ -n "${missing_vars}" ]]; then
    echo "Error: Some environment variables are missing:"
    echo "${missing_vars}"
    exit 1
else
    exit 0
fi
