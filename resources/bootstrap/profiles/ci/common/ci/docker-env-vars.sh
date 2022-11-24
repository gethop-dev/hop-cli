#!/usr/bin/env bash

set -eu -o pipefail

vars="$(docker-compose config)"
if [[ $(echo "${vars}" | grep -Fc ': null') -gt 0 ]]; then
    echo "Error: Some environment variables are missing:"
    echo "${vars}" | awk -F':' '/: null/ {print $1}'
    exit 1
else
    exit 0
fi
