(ns hop-cli.bootstrap.profile.docker)

(defn- build-postgres-env-variables
  [_settings _environment]
  {:POSTGRES_HOST ""
   :POSTGRES_PORT ""
   :POSTGRES_DB ""
   :POSTGRES_USER ""
   :POSTGRES_PASSWORD ""
   :APP_DB_USER ""
   :APP_DB_PASSWORD ""
   :APP_DB_SCHEMA ""})

(defn- build-memory-limit-env-variables
  [_settings _environment]
  {:MEMORY_LIMIT_APP ""
   :MEMORY_LIMIT_NGINX ""
   :MEMORY_LIMIT_KEYCLOAK ""
   :MEMORY_LIMIT_GRAFANA ""})

(defn- build-keycloak-env-variables
  [_settings _environment]
  {:KEYCLOAK_USER ""
   :KEYCLOAK_PASSWORD ""
   :KEYCLOAK_DB_VENDOR ""
   :KEYCLOAK_DB_ADDR= ""
   :KEYCLOAK_DB_PORT ""
   :KEYCLOAK_DB_DATABASE ""
   :KEYCLOAK_DB_USER ""
   :KEYCLOAK_DB_PASSWORD ""
   :KEYCLOAK_DB_SCHEMA ""
   :KEYCLOAK_PROXY_ADDRESS_FORWARDING true

   :KEYCLOAK_REALM ""
   :KEYCLOAK_FRONTEND_URL ""
   :KEYCLOAK_CLIENT_ID ""})

(defn- build-grafana-env-variables
  [_settings _environment]
  {:GF_SERVER_DOMAIN ""
   :GF_SERVER_HTTP_PORT ""
   :GF_SERVER_ROOT_URL ""

   :GF_AUTH_ANONYMOUS_ENABLED ""
   :GF_AUTH_DISABLE_SIGNOUT_MENU ""
   :GF_SECURITY_ADMIN_USER ""
   :GF_SECURITY_ADMIN_PASSWORD ""
   :GF_SECURITY_ALLOW_EMBEDDING ""

   :GF_AUTH_GENERIC_OAUTH_ENABLED ""
   :GF_AUTH_GENERIC_OAUTH_CLIENT_ID ""
   :GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET ""
   :GF_AUTH_GENERIC_OAUTH_SEND_CLIENT_CREDENTIALS_VIA_POST ""
   :GF_AUTH_GENERIC_OAUTH_ALLOW_SIGN_UP ""

   :GF_AUTH_GENERIC_OAUTH_AUTH_URL ""
   :GF_AUTH_GENERIC_OAUTH_TOKEN_URL ""
   :GF_AUTH_GENERIC_OAUTH_API_URL ""

   :GF_AUTH_OAUTH_AUTO_LOGIN ""
   :GF_AUTH_SIGNOUT_REDIRECT_URL ""
   :GF_AUTH_GENERIC_OAUTH_SCOPES ""

   :GF_DATABASE_TYPE ""
   :GF_DATABASE_HOST ""
   :GF_DATABASE_NAME ""
   :GF_DATABASE_SCHEMA ""
   :GF_DATABASE_USER ""
   :GF_DATABASE_PASSWORD ""
   :GF_DATABASE_SSL_MODE "disable"

   :GF_DATASOURCE_NAME ""
   :GF_DATASOURCE_TYPE ""
   :GF_DATASOURCE_ACCESS ""
   :GF_DATASOURCE_URL ""
   :GF_DATASOURCE_DATABASE ""
   :GF_DATASOURCE_USER ""
   :GF_DATASOURCE_PASSWORD ""
   :GF_DATASOURCE_SSL ""
   :GF_DATASOURCE_MAX_OPEN_CONNS ""
   :GF_DATASOURCE_VERSION ""
   :GF_DATASOURCE_TIMESCALEDB ""})

(defn- build-env-variables
  [settings environment]
  (merge
   (build-postgres-env-variables settings environment)
   (build-memory-limit-env-variables settings environment)
   (build-keycloak-env-variables settings environment)
   (build-grafana-env-variables settings environment)))

(defn profile
  [settings]
  {:files [{:src "docker"}]
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}})
