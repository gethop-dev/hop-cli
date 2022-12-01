#!/usr/bin/env bash

set -eu -o pipefail

BABASHKA_VERSION=0.10.163
INSTALL_DIR="/usr/local/babashka"
DOWNLOAD_DIR="${INSTALL_DIR}/${BABASHKA_VERSION}"
DOWNLOAD_PATH="${DOWNLOAD_DIR}/bb.tar.gz"

trap 'rm -rf ${DOWNLOAD_DIR}' ERR

if [ ! -d "${DOWNLOAD_DIR}" ]; then
    mkdir --parents "${DOWNLOAD_DIR}"
    curl --location --output "${DOWNLOAD_PATH}" "https://github.com/babashka/babashka/releases/download/v${BABASHKA_VERSION}/babashka-${BABASHKA_VERSION}-linux-amd64.tar.gz"
    tar --extract --directory "${DOWNLOAD_DIR}" --file "${DOWNLOAD_PATH}"
    chmod +x "${DOWNLOAD_DIR}/bb"
fi

ln --symbolic --force "${DOWNLOAD_DIR}/bb" "/usr/local/bin/"
