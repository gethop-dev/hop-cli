#!/usr/bin/env bash

set -eu -o pipefail

docker run --rm \
    --workdir "/project" -v "${PWD}:/project" \
    magnetcoop/bob:229edfdbdb \
    sh -c "all-checks.sh"
