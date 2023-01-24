#!/usr/bin/env bash

set -eu -o pipefail

APP_HTTP_PORT=8081
HEALTHCHECK_URL="http://localhost:${APP_HTTP_PORT}/api/config"
HEALTHCHECK_CONNECT_TIMEOUT=5
HEALTHCHECK_REQUEST_TIMEOUT=5

# The application takes around 30-45 seconds to start (JVM is
# initialized twice, one for executing migrations and one for the web
# server), so don't check the service more often than that, as the
# service might still be starting from last failed check. We put a
# safe number here.
HEALTHCHECK_INTERVAL=90

function usage() {
    local scriptname
    scriptname=$(basename "$0")
    cat <<EOF

Usage: ${scriptname} {start|stop|restart}

EOF
}

function start() {
    cd "${HOME}/app-files"
    ./start-app-prod.sh
}

function stop() {
    cd "${HOME}/app-files"
    ./stop-app-prod.sh
}

function do_healthcheck() {
    # Add '--insecure' if using self-signed certificate
    curl --silent --output /dev/null --fail \
        --connect-time "${HEALTHCHECK_CONNECT_TIMEOUT}" \
        --max-time "${HEALTHCHECK_REQUEST_TIMEOUT}" "${HEALTHCHECK_URL}"
}

function healthcheck() {
    # Wait before first health check, to let the application service start
    # up and be ready to accept requests.
    sleep "${HEALTHCHECK_INTERVAL}"

    while true; do
        if do_healthcheck; then
            # Don't print anything, to avoid spamming the logs with
            # useless info.
            sleep "${HEALTHCHECK_INTERVAL}"
        else
            # We should probably create a Metric in AWS Cloudwatch and
            # publish data about health status there. And configure an
            # alarm based on that metric and set some notification
            # action based on that alarm
            echo "Health check status failed! Exiting..."
            exit 1
        fi
    done
}

if [ $# -lt 1 ]; then
    usage
    exit 1
fi

if [ -z "${PERSISTENT_DATA_DIR}" ]; then
    echo "PERSISTENT_DATA_DIR env variable is not defined. Exiting..."
    exit 1
fi

case "$1" in
"start")
    echo "Starting {{project.name}} Application"
    start &
    healthcheck
    ;;
"stop")
    echo "Stopping {{project.name}} Application"
    stop
    exit 0
    ;;
"restart")
    $0 stop
    $0 start
    ;;
*)
    usage
    exit 1
    ;;
esac

# We shouldn't reache this point. If we do, something went wrong, so
# exit with failure
exit 1
