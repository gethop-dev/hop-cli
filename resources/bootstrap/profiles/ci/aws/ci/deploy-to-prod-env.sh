#!/usr/bin/env bash

set -eu -o pipefail

# Update test environment
aws elasticbeanstalk update-environment \
    --application-name "${APPLICATION_NAME}" \
    --environment-name "${PROD_ENV_NAME}" \
    --version-label "${VERSION_LABEL}"
