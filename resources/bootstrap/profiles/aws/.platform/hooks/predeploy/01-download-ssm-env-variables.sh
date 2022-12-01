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

get_var() {
    awk --field-separator '=' \
        --assign=var="^${1}=" \
        '$0~var {gsub("^ +$", "", $2); print $2}' \
        "${NEW_ENV_FILE_PATH}"
}

PROJECT_NAME="$(get_var "PROJECT_NAME")"
ENVIRONMENT="$(get_var "ENVIRONMENT")"
KMS_KEY_ALIAS="$(get_var "KMS_KEY_ALIAS")"

if [[ -z "${PROJECT_NAME}" || -z "${ENVIRONMENT}" || -z "${KMS_KEY_ALIAS}" ]]; then
    echo "Some environment variable(s) required by the CLI has an empty value."
    echo "Aborting..."
    exit 1
fi

TEMP_FILE=$(mktemp)

trap 'rm -f ${TEMP_FILE}' EXIT ERR

bb /usr/local/hop-cli/hop-cli.jar \
    aws \
    env-vars \
    download \
    --project-name "${PROJECT_NAME}" \
    --environment "${ENVIRONMENT}" \
    --kms-key-alias "${KMS_KEY_ALIAS}" \
    --file "${TEMP_FILE}"

cat "${TEMP_FILE}" >>"${NEW_ENV_FILE_PATH}"
