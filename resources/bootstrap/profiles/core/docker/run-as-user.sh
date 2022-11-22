#!/usr/bin/env bash

set -eu

NEW_UID=$(stat -c '%u' /app)
NEW_GID=$(stat -c '%g' /app)

groupmod -g "$NEW_GID" -o hop >/dev/null 2>&1
usermod -u "$NEW_UID" -o hop >/dev/null 2>&1

exec chpst -u hop:hop -U hop:hop env HOME="/home/hop" "$@"
