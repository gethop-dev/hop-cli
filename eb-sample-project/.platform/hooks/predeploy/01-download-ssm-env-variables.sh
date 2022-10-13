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

# TODO: there is probably a better way of doing this.
while read line; do
    if [[ ! ${line:0:1} = "#" ]]; then
        KEY=${line%=*}
        VALUE=${line#*=}
        case ${KEY} in
            "PROJECT_NAME")
                PROJECT_NAME="${VALUE}"
                ;;
            "ENVIRONMENT")
                ENVIRONMENT="${VALUE}"
                ;;
            "KMS_KEY_ALIAS")
                KMS_KEY_ALIAS="${VALUE}"
                ;;
            *)
                echo "Skipping environment variable ${KEY}"
                ;;
        esac
    fi
done < ${NEW_ENV_FILE_PATH}

TEMP_FILE=$(mktemp)

trap "rm -f ${TEMP_FILE}" EXIT ERR

bb /usr/local/ssm-parameter-store-sync/ssm-parameter-store-sync.jar \
   download-env-vars \
   -p "${PROJECT_NAME}" -e "${ENVIRONMENT}" -k "${KMS_KEY_ALIAS}" -f "${TEMP_FILE}"
cat "${TEMP_FILE}" >> ${NEW_ENV_FILE_PATH}
