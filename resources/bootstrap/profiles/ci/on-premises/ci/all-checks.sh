#!/usr/bin/env bash

set -eu -o pipefail

docker run --rm \
    --workdir "/project" -v "${PWD}:/project" \
    magnetcoop/bob:2e3b8bc \
    sh -c "all-checks.sh"
