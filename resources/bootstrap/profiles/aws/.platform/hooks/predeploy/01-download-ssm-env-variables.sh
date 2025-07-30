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

# Use instance meta-data service (IMDSv2) to get the instance region.
# See
# https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html#instance-metadata-v2-how-it-works
# and
# https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
# for details. Gettting a token that only lasts 60 seconds should be more than enough.
IMDSv2_TOKEN_DURATION_SECS=60
IMDSv2_TOKEN="$(curl -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: ${IMDSv2_TOKEN_DURATION_SECS}")"
if [[ -z "${IMDSv2_TOKEN}" ]]; then
    echo "Could not get a IMDSv2 Token to try and determine EC2 instance AWS Region."
    echo "Aborting..."
    exit 1
fi

REGION="$(curl -H "X-aws-ec2-metadata-token: ${IMDSv2_TOKEN}" http://169.254.169.254/latest/meta-data/placement/region)"
if [[ -z "${REGION}" ]]; then
    echo "Could not determine EC2 instance AWS Region. Aborting..."
    exit 1
fi

/usr/local/bin/bb /usr/local/hop-cli/hop-cli.jar \
    aws \
    env-vars \
    download \
    --project-name "${PROJECT_NAME}" \
    --environment "${ENVIRONMENT}" \
    --kms-key-alias "${KMS_KEY_ALIAS}" \
    --file "${TEMP_FILE}" \
    --region "${REGION}"

cat "${TEMP_FILE}" >>"${NEW_ENV_FILE_PATH}"
