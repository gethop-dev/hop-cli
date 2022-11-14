(ns hop-cli.bootstrap.profile.registry.bi.grafana)

(defn- dashboard-manager-adapter-config
  [_settings]
  {:dev.gethop.dashboard-manager/grafana
   {:uri (tagged-literal 'duct/env ["GRAFANA_URI" 'Str])
    :credentials [(tagged-literal 'duct/env ["GRAFANA_USERNAME" 'Str])
                  (tagged-literal 'duct/env ["GRAFANA_TEST_PASSWORD" 'Str])]}})

(defn- sso-apps-config
  []
  {:sso-apps [{:name (tagged-literal 'duct/env ["OIDC_SSO_APP_1_NAME" 'Str])
               :login-url (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGIN_URL" 'Str])
               :login-method (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGIN_METHOD" 'Str])
               :logout-url (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGOUT_URL" 'Str])
               :logout-method (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGOUT_METHOD" 'Str])}]})

(defn- build-env-variables
  [_settings _environment]
  {:DS_MANAGER_URI "uri"
   :DS_MANAGER_CREDENTIALS_USER "user"
   :DS_MANAGER_CREDENTIALS_PASSWORD "password"})

(defn profile
  [settings]
  {:dependencies '[[dev.gethop/dashboard-manager.grafana "0.2.8"]]
   :config-edn {:base (dashboard-manager-adapter-config settings)
                :config (sso-apps-config)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files [{:src "bi/grafana/grafana"
            :dst "grafana"}]
   :docker-compose {:base ["docker-compose.grafana.yml"]
                    :dev ["docker-compose.common-dev-ci.grafana.yml"]
                    :ci ["docker-compose.common-dev-ci.grafana.yml"]}})
