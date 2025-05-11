#!/usr/bin/env bash

set -eu -o pipefail

docker run --rm \
    --workdir "/project" -v "${PWD}:/project" \
    magnetcoop/bob:56421c5838 \
    sh -c "all-checks.sh"
