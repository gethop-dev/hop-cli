-- SQL for creating the schemas and roles used by the project.
-- This SQL script must be executed manually using the admin credentials:
-- DB name: ${DB_NAME}
-- User: ${DB_ADMIN_USER}
-- Password: ${DB_ADMIN_PASSWORD}

{{>bootstrap/profiles/persistence/sql/postgres/init-scripts/create_schemas_and_roles.app.sql}}

{{#project.profiles.persistence-sql.deployment.to-deploy.rds.environment.test.database.other-users.keycloak.enabled}}
{{>bootstrap/profiles/persistence/sql/postgres/init-scripts/create_schemas_and_roles.keycloak.sql}}
{{/project.profiles.persistence-sql.deployment.to-deploy.rds.environment.test.database.other-users.keycloak.enabled}}

{{#project.profiles.persistence-sql.deployment.to-deploy.rds.environment.test.database.other-users.grafana.enabled}}
{{>bootstrap/profiles/persistence/sql/postgres/init-scripts/create_schemas_and_roles.grafana.sql}}
{{/project.profiles.persistence-sql.deployment.to-deploy.rds.environment.test.database.other-users.grafana.enabled}}
