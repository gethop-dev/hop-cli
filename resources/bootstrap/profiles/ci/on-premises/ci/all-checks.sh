#!/usr/bin/env bash

set -eu -o pipefail

docker run --rm \
    --workdir "/project" -v "${PWD}:/project" \
    magnetcoop/bob:6ba126e0f0 \
    sh -c "all-checks.sh"
