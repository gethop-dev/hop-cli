#!/usr/bin/env bash

set -eu -o pipefail

PROJECT_NAME=project
ENVIRONMENT=test
KMS_KEY_ALIAS=alias/Hydrogen

TEMP_FILE=$(mktemp)

trap "rm -f ${TEMP_FILE}" EXIT ERR

bb /usr/local/ssm-parameter-store-sync/ssm-parameter-store-sync.jar \
   download-env-vars \
   -p "${PROJECT_NAME}" -e "${ENVIRONMENT}" -k "${KMS_KEY_ALIAS}" -f "${TEMP_FILE}"
cat "${TEMP_FILE}" >> .env
