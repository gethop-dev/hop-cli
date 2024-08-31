#!/usr/bin/env bash
#shellcheck disable=SC1010

set -eu -o pipefail

# Tell docker-compose what compose files to use to run the different commands below
# Order of the files IS IMPORTANT, as later files overwrite values from previous ones.
# See https://docs.docker.com/compose/reference/envvars/#compose_file
export COMPOSE_FILE="{{project.docker-compose.ci}}"

docker build --target ci --tag lint-and-test .

docker/docker-compose.sh up -d

echo "clj-kondo"
time docker/docker-compose.sh exec -T app clj-kondo --lint src --lint test --lint dev

echo "cljfmt"
time docker/docker-compose.sh exec -T app lein cljfmt check

echo "eastwood"
time docker/docker-compose.sh exec -T app lein eastwood

echo "tests"
time docker/docker-compose.sh exec -T app lein test :all

docker/docker-compose.sh down -t0
