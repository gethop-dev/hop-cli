;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry.observability.cloudwatch-logging
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- build-module-config
  [_settings]
  {:dev.gethop.timbre.appenders/cloudwatch
   {:log-group-name
    (tagged-literal 'duct/env ["CLOUDWATCH_LOG_GROUP" 'Str])

    :appender-config
    {:min-level (tagged-literal 'duct/env ["CLOUDWATCH_LOG_LEVEL" 'Keyword])}

    :batch-config
    {:size 1000 :timeout 300}}})

(defn- build-env-variables
  [settings environment]
  (let [env-path [:project :profiles :observability-cloudwatch-logging
                  :environment environment]]
    {:CLOUDWATCH_LOG_GROUP
     (bp.util/get-settings-value settings (conj env-path :log-group-name))
     :CLOUDWATCH_LOG_LEVEL
     (bp.util/get-settings-value settings (conj env-path :min-log-level))}))

(defmethod registry/pre-render-hook :observability-cloudwatch-logging
  [_ settings]
  {:dependencies '[[dev.gethop/timbre.appenders.cloudwatch "0.1.2"]]
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :extra-app-docker-compose-environment-variables ["CLOUDWATCH_LOG_GROUP"
                                                    "CLOUDWATCH_LOG_LEVEL"]
   :config-edn {:modules (build-module-config settings)}})
