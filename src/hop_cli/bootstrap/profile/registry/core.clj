(ns hop-cli.bootstrap.profile.registry.core)

(defn- common-config
  []
  {:logger (tagged-literal 'ig/ref :duct/logger)})

(defn- build-env-variables
  [_settings _environment]
  {:MEMORY_LIMIT_APP "1024m"
   :MEMORY_LIMIT_PROXY "128m"})

(def ^:const docker-compose-files
  {:to-develop
   ["docker-compose.core.yml"
    "docker-compose.core.dev.yml"]
   :ci
   ["docker-compose.core.yml"
    "docker-compose.core.ci.yml"]
   :to-deploy
   ["docker-compose.core.yml"
    "docker-compose.core.to-deploy.yml"]})

(defn profile
  [settings]
  {:files [{:src "core"}]
   :config-edn {:common-config (common-config)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :docker-compose docker-compose-files
   :deploy-files ["proxy"]})
