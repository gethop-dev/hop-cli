#!/usr/bin/env bash

set -eu -o pipefail

# This .env is new, created by AWS EB machinery and populated with the
# environment variables we add from the AWS console. As of now
# (2022-10-13) AWS puts the application in a staging folder
# (/var/app/staging) and then copies everything to a current folder
# (/var/app/current). This script is executed at the root of the
# staging folder where the .env file is located. So, we are using this
# .env file to get the environment variables required to call SSM
# Parameter Store and retrieve all the environment variables for the
# project. Finally we append the SSMPS result to the same .env file
# which will be copied by the EB system to the current folder.
NEW_ENV_FILE_PATH="${PWD}/.env"
PROJECT_NAME="$(awk --field-separator '=' '/^PROJECT_NAME=/ {gsub("^ +$", "", $2); print $2}' "${NEW_ENV_FILE_PATH}")"
ENVIRONMENT="$(awk --field-separator '=' '/^ENVIRONMENT=/ {gsub("^ +$", "", $2); print $2}' "${NEW_ENV_FILE_PATH}")"
KMS_KEY_ALIAS="$(awk --field-separator '=' '/^KMS_KEY_ALIAS=/ {gsub("^ +$", "", $2); print $2}' "${NEW_ENV_FILE_PATH}")"

if [[ -z "${PROJECT_NAME}" || -z "${ENVIRONMENT}" || "${KMS_KEY_ALIAS}" ]]; then
    echo "Some environment variable(s) required by the CLI has an empty value."
    echo "Aborting..."
    exit 1
fi

TEMP_FILE=$(mktemp)

trap "rm -f ${TEMP_FILE}" EXIT ERR

bb /usr/local/ssm-parameter-store-sync/ssm-parameter-store-sync.jar \
   download-env-vars \
   -p "${PROJECT_NAME}" -e "${ENVIRONMENT}" -k "${KMS_KEY_ALIAS}" -f "${TEMP_FILE}"
cat "${TEMP_FILE}" >> ${NEW_ENV_FILE_PATH}
