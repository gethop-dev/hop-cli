(ns hop-cli.bootstrap.profile.registry.core)

(defn- common-config
  []
  {:logger (tagged-literal 'ig/ref :duct/logger)})

(defn- build-env-variables
  [_settings _environment]
  {:MEMORY_LIMIT_APP "1024m"
   :MEMORY_LIMIT_PROXY "128m"})

(defn profile
  [settings]
  {:files [{:src "core"}]
   :config-edn {:common-config (common-config)}
   :docker-compose {:base ["docker-compose.yml"]
                    :dev ["docker-compose.dev.yml"]
                    :ci ["docker-compose.ci.yml"]}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}})
