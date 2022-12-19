;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.infrastructure
  (:require [hop-cli.bootstrap.util :as bp.util]))

(defn get-cloud-provider-key
  [settings]
  (bp.util/get-settings-value settings :cloud-provider/value))

(defmulti provision-initial-infrastructure get-cloud-provider-key)

(defmulti save-environment-variables get-cloud-provider-key)
