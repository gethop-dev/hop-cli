(ns hop-cli.bootstrap.infrastructure
  (:require [hop-cli.bootstrap.util :as bp.util]))

(defn get-cloud-provider-key
  [settings]
  (bp.util/get-settings-value settings :cloud-provider/value))

(defmulti provision-initial-infrastructure get-cloud-provider-key)

(defmulti save-environment-variables get-cloud-provider-key)
