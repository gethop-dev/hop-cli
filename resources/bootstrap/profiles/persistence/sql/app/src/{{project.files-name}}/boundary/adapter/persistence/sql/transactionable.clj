;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.boundary.adapter.persistence.sql.transactionable
  (:require [<<project.name>>.boundary.adapter.persistence.connector]
            [<<project.name>>.boundary.adapter.persistence.sql.jdbc-util :as sql.jdbc-util]
            [<<project.name>>.boundary.port.persistence :as persistence]
            [next.jdbc :as jdbc])
  (:import [<<project.files-name>>.boundary.adapter.persistence.connector Sql]))

(defn- with-transaction*
  ([config body-fn body-fn-argv]
   (with-transaction* config body-fn body-fn-argv (comp not :success?)))
  ([{:keys [logger] {:keys [db-spec]} :p-adapter :as config}
    body-fn body-fn-argv rollback-check-fn]
   (try
     (jdbc/with-transaction [tx-conn db-spec]
       (let [tx-conn-w-opts+logging (sql.jdbc-util/with-options+logging
                                      tx-conn logger)
             altered-config (assoc-in config [:p-adapter :db-spec] tx-conn-w-opts+logging)
             transaction-result (apply body-fn (into [altered-config] body-fn-argv))]
         (if-not (rollback-check-fn transaction-result)
           transaction-result
           (do
             (.rollback tx-conn)
             transaction-result))))
     (catch Throwable t
       {:success? false
        :reason :exception
        :error-details {:ex-message (ex-message t)}}))))

(extend-protocol persistence/Transactionable
  Sql
  (with-transaction
    ([_ config body-fn body-fn-argv]
     (with-transaction* config body-fn body-fn-argv))
    ([_ config body-fn body-fn-argv rollback-check-fn]
     (with-transaction* config body-fn body-fn-argv rollback-check-fn))))
