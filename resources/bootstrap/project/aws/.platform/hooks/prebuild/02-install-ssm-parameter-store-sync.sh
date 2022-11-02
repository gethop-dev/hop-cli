#!/usr/bin/env bash

set -eu -o pipefail

SSM_SYNC_VERSION=0.1.0
SSM_SYNC_NAME=ssm-parameter-store-sync.jar
INSTALL_DIR="/usr/local/ssm-parameter-store-sync"
DOWNLOAD_DIR="${INSTALL_DIR}/${SSM_SYNC_VERSION}"
DOWNLOAD_PATH="${DOWNLOAD_DIR}/${SSM_SYNC_NAME}"

trap "rm -rf ${DOWNLOAD_DIR}" ERR

if [ ! -d "${DOWNLOAD_DIR}" ];
then
    mkdir --parents "${DOWNLOAD_DIR}"
    curl --location --output "${DOWNLOAD_PATH}" "https://github.com/gethop-dev/aws.cloudformation/releases/download/${SSM_SYNC_VERSION}/ssm-parameter-store-sync.jar"
fi

ln --symbolic --force "${DOWNLOAD_PATH}" "${INSTALL_DIR}/${SSM_SYNC_NAME}"
