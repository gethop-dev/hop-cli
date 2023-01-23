#!/usr/bin/env bash
#shellcheck disable=SC2034

set -eu -o pipefail

SCRIPT_DIR=$(dirname "$(realpath "$0")")
SCRIPT_PARENT_DIR=$(dirname "${SCRIPT_DIR}")
# shellcheck disable=SC1090,SC1091
source "${SCRIPT_PARENT_DIR}/common-vars.sh"

SOURCE_BUNDLE_FILES=({{#project.deploy-files}}{{&.}}{{/project.deploy-files}})

APPLICATION_NAME={{project.files-name}}
