(ns hop-cli.bootstrap.profile.core)

(defn- common-config
  []
  {:logger (tagged-literal 'ig/ref :duct/logger)})

(defn profile
  [_settings]
  {:files [{:src "core"}]
   :config-edn {:common-config (common-config)}})
