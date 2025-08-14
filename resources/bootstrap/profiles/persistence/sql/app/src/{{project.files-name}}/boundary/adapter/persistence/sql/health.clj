;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.boundary.adapter.persistence.sql.health
  (:require [<<project.name>>.boundary.adapter.persistence.connector]
            [<<project.name>>.boundary.port.persistence :as persistence]
            [diehard.core :as dh]
            [next.jdbc :as jdbc])
  (:import [<<project.files-name>>.boundary.adapter.persistence.connector Sql]
           [dev.failsafe TimeoutExceededException]))

(defn get-status
  [db-spec {:keys [timeout-ms] :or {timeout-ms 2000}}]
  (try
    (dh/with-timeout {:timeout-ms timeout-ms
                      :interrupt? true}
      (with-open [conn (jdbc/get-connection db-spec)]
        {:healthy? (.isValid conn 0)}))
    (catch TimeoutExceededException _
      {:healthy? false
       :reason :timeout-exceeded})
    (catch Throwable t
      {:healthy? false
       :reason :exception
       :error-details {:ex-message (ex-message t)}})))

(extend-protocol persistence/Health
  Sql
  (get-status [{:keys [db-spec]} opts]
    (get-status db-spec opts)))
