#!/usr/bin/env bash

set -eu -o pipefail

HOP_CLI_VERSION=0.1.0-alpha-2
HOP_CLI_NAME=hop-cli.jar
INSTALL_DIR="/usr/local/hop-cli"
DOWNLOAD_DIR="${INSTALL_DIR}/${HOP_CLI_VERSION}"
DOWNLOAD_PATH="${DOWNLOAD_DIR}/${HOP_CLI_NAME}"

trap 'rm -rf ${DOWNLOAD_DIR}' ERR

if [ ! -d "${DOWNLOAD_DIR}" ]; then
    mkdir --parents "${DOWNLOAD_DIR}"
    curl --location --output "${DOWNLOAD_PATH}" "https://github.com/gethop-dev/hop-cli/releases/download/${HOP_CLI_VERSION}/hop-cli.jar"
fi

ln --symbolic --force "${DOWNLOAD_PATH}" "${INSTALL_DIR}/${HOP_CLI_NAME}"
