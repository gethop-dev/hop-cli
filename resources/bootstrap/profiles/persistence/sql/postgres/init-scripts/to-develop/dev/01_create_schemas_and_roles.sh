#!/usr/bin/env bash

set -eu

psql -v ON_ERROR_STOP=1 --username "${DB_ADMIN_USER}" --dbname="${DB_NAME}" <<-EOSQL
{{>bootstrap/profiles/persistence/sql/postgres/init-scripts/create_schemas_and_roles.app.sql}}

{{#project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.keycloak.enabled}}
{{>bootstrap/profiles/persistence/sql/postgres/init-scripts/create_schemas_and_roles.keycloak.sql}}
{{/project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.keycloak.enabled}}

{{#project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.grafana.enabled}}
{{>bootstrap/profiles/persistence/sql/postgres/init-scripts/create_schemas_and_roles.grafana.sql}}
{{/project.profiles.persistence-sql.deployment.to-develop.container.environment.dev.database.other-users.grafana.enabled}}

EOSQL
