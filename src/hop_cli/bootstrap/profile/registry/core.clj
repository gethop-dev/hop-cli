(ns hop-cli.bootstrap.profile.registry.core
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- common-config
  []
  {:logger (tagged-literal 'ig/ref :duct/logger)})

(defn- build-env-variables
  [settings environment]
  (let [base-path [:project :profiles :core :environment environment]]
    {:MEMORY_LIMIT_APP
     (str (bp.util/get-settings-value settings (conj base-path :app-memory-limit-mb)) "m")
     :MEMORY_LIMIT_PROXY
     (str (bp.util/get-settings-value settings (conj base-path :nginx-memory-limit-mb)) "m")}))

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

(defmethod registry/pre-render-hook :core
  [_ settings]
  {:files [{:src "core"}]
   :config-edn {:common-config (common-config)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :docker-compose docker-compose-files
   :deploy-files ["proxy"]})
