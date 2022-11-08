(ns hop-cli.bootstrap.profile.bi.grafana)

(defn- dashboard-manager-adapter-config
  [_settings]
  {:dev.gethop.dashboard-manager/grafana
   {:uri (tagged-literal 'duct/env ["GRAFANA_URI" 'Str])
    :credentials [(tagged-literal 'duct/env ["GRAFANA_USERNAME" 'Str])
                  (tagged-literal 'duct/env ["GRAFANA_TEST_PASSWORD" 'Str])]}})

(defn- build-env-variables
  [settings environment]
  {:DS_MANAGER_URI "uri"
   :DS_MANAGER_CREDENTIALS_USER "user"
   :DS_MANAGER_CREDENTIALS_PASSWORD "password"})

(defn profile
  [settings]
  {:dependencies '[[dev.gethop/dashboard-manager.grafana "0.2.8"]]
   :config-edn {:base (dashboard-manager-adapter-config settings)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files [{:src "bi/grafana/grafana"
            :dst "grafana"
            :copy-if (constantly true)}]})
