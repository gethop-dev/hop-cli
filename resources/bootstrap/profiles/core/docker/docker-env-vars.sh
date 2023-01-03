#!/usr/bin/env bash

set -eu -o pipefail

function get_missing_env_vars() {
    if docker-compose config --no-normalize >/dev/null 2>/dev/null; then
        # docker-compose v2.x
        COMPAT_ARG='--no-normalize'
    else
        # docker-compose v1.x
        COMPAT_ARG=''
    fi

    #shellcheck disable=SC2086
    docker-compose config ${COMPAT_ARG} |
        docker run --rm -i mikefarah/yq '.services[].environment?
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
                    .key  "="  ">>HOP_ENV_VAR_MISSING_VALUE<<"
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
