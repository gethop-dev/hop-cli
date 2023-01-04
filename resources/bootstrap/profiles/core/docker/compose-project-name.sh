#!/usr/bin/env bash

set -eu -o pipefail

# docker-compose versions prior to 1.21.0 stripped dashes and
# underscores from project names. Later versions don't. As we want to
# build the same project name docker-compose would, we need to use a
# slightly different sed expression depending on the docker-compose
# version we are using.
compose_version="$(docker-compose --version |
    sed 's/docker-compose version \([0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*\).*/\1/g')"
if [[ "${compose_version}" < "1.21.0" ]]; then
    SED_EXPR='s/[^a-z0-9]//g'
else
    SED_EXPR='s/[^-_a-z0-9]//g'
fi

# Tell docker-compose to use the (lowercased) project name as the
# containers project name. 'readlink -f' gives us the absolute path to
# this script. We get the full directory path from it; with 'dirname'
# we get the directory for this script, and again the `dirname` to get
# the parent directory for the script. From there we take the simple
# name (base name) to get the name of the parent directory, where all
# the docker-compose files are.
DIR_NAME="$(basename "$(dirname "$(dirname "$(readlink -f "$0")")")")"
COMPOSE_PROJECT="$(echo -n "${DIR_NAME}" | tr '[:upper:]' '[:lower:]' | sed "${SED_EXPR}")"

# Write project name to stdout, so the calling script can capture it.
echo "${COMPOSE_PROJECT}"
