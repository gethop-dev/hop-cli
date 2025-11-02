;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.util.thread-transactions
  (:require [duct.logger :refer [log]]
            [malli.core :as m]))

(def ^:private transaction-schema
  [:map
   [:txn-fn fn?]
   [:rollback-fn {:optional true} fn?]])

(def ^:private transactions-schema
  [:sequential {:min 0} transaction-schema])

(def ^:private args-map-schema
  [:map])

(defn- safe-run
  [logger f m]
  (try
    (f m)
    (catch Throwable e
      (log logger :error ::tht-transaction-exception {:reason (str (class e))
                                                      :message (ex-message e)
                                                      :stack-trace (map str (.getStackTrace e))})
      (merge m {:success? false
                :error-details {:reason (str (class e))
                                :message (ex-message e)}}))))

(defn thread-transactions
  [logger txns args-map]
  {:pre [(m/validate transactions-schema txns)
         (m/validate args-map-schema args-map)]}
  (let [txn-fns (mapv :txn-fn txns)
        rollback-fns (mapv (fn [txn]
                             (or (:rollback-fn txn) identity))
                           (reverse txns))
        [step txn-fns-result]
        (reduce (fn [[step args-map] txn-fn]
                  (let [result (safe-run logger txn-fn args-map)]
                    (cond
                      (not (:success? result))
                      (reduced [step result])

                      (:stop-txn-fn-processing result)
                      (reduced [step (dissoc result :stop-txn-fn-processing)])

                      :else
                      [(inc step) result])))
                [0 args-map]
                txn-fns)]
    (if (:success? txn-fns-result)
      txn-fns-result
      (let [rollback-fns-to-run (nthrest rollback-fns (- (count rollback-fns) step))]
        (reduce (fn [args-map rollback-fn]
                  (let [result (safe-run logger rollback-fn args-map)]
                    (if (:stop-rollback-fn-processing result)
                      (reduced (-> (assoc result :success? false)
                                   (dissoc :stop-rollback-fn-processing)))
                      (assoc result :success? false))))
                txn-fns-result
                rollback-fns-to-run)))))
