;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.infrastructure
  (:require [hop-cli.bootstrap.util :as bp.util]))

(defn get-deployment-target-key
  [settings]
  (bp.util/get-settings-value settings :deployment-target/value))

(defmulti provision-initial-infrastructure get-deployment-target-key)

(defmulti save-environment-variables get-deployment-target-key)

(defmethod provision-initial-infrastructure :default
  [settings]
  {:success? true
   :settings settings})

(defmethod save-environment-variables :default
  [settings]
  {:success? true
   :saved-env-files (bp.util/write-environments-env-vars-to-file! settings)})
