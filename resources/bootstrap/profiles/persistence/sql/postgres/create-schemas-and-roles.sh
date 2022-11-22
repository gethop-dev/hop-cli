#!/usr/bin/env bash

set -eu

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname="${POSTGRES_DB}" <<-EOSQL
CREATE ROLE ${APP_DB_USER} WITH LOGIN PASSWORD '${APP_DB_PASSWORD}';
GRANT ${APP_DB_USER} TO ${POSTGRES_USER};
CREATE SCHEMA IF NOT EXISTS ${APP_DB_SCHEMA} AUTHORIZATION ${APP_DB_USER};
ALTER ROLE ${APP_DB_USER} SET search_path TO ${APP_DB_SCHEMA};

{{#project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.keycloak.username}}
CREATE ROLE ${KC_DB_USERNAME} WITH LOGIN PASSWORD '${KC_DB_PASSWORD}';
GRANT ${KC_DB_USERNAME} TO ${POSTGRES_USER};
CREATE SCHEMA IF NOT EXISTS ${KC_DB_SCHEMA} AUTHORIZATION ${KC_DB_USERNAME};
ALTER ROLE ${KC_DB_USERNAME} SET search_path TO ${KC_DB_SCHEMA};
{{/project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.keycloak.username}}

{{#project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.grafana.username}}
CREATE ROLE ${GF_DATABASE_USER} WITH LOGIN PASSWORD '${GF_DATABASE_PASSWORD}';
GRANT ${GF_DATABASE_USER} TO ${POSTGRES_USER};
CREATE SCHEMA IF NOT EXISTS ${GF_DATABASE_SCHEMA} AUTHORIZATION ${GF_DATABASE_USER};
ALTER ROLE ${GF_DATABASE_USER} SET search_path TO ${GF_DATABASE_SCHEMA};
{{/project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.grafana.username}}

{{#project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.grafana_datasource.username}}
CREATE ROLE ${GF_DATASOURCE_USER} WITH LOGIN PASSWORD '${GF_DATASOURCE_PASSWORD}';
GRANT USAGE ON SCHEMA ${APP_DB_SCHEMA} TO ${GF_DATASOURCE_USER};
GRANT SELECT ON ALL TABLES IN SCHEMA ${APP_DB_SCHEMA} TO ${GF_DATASOURCE_USER};
ALTER DEFAULT PRIVILEGES FOR ROLE ${APP_DB_USER} IN SCHEMA ${APP_DB_SCHEMA} GRANT SELECT ON TABLES TO ${GF_DATASOURCE_USER};
ALTER ROLE ${GF_DATASOURCE_USER} SET search_path TO ${APP_DB_SCHEMA};
{{/project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.grafana_datasource.username}}
EOSQL
