#!/usr/bin/env bash
#shellcheck disable=SC2091

set -eu

$(aws ecr get-login --no-include-email --region "${AWS_DEFAULT_REGION}" | sed 's/\r$//')
