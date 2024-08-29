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
                                                      :message (.getMessage e)
                                                      :stack-trace (map str (.getStackTrace e))})
      (merge m {:success? false
                :error-details {:reason (str (class e))
                                :message (.getMessage e)}}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; The use of recursion (instead of using `recur`, or `trampoline`)
;; for the implementation of `thread-transactions` is *intentional*.
;;
;; The reason why is that we need to run the rollback functions (in
;; the reverse order) for all the "forward functions" that were
;; executed up to the point when we hit a failure. And for that we
;; need to keep track of those rollback functions in something that
;; can work in a LIFO manner (to run then in the reverse order).
;;
;; Than can be something that we manually manage ourselves[1] (and
;; then use `recur` or `trampoline` or whatever). Or simply use
;; regular recursion and let the language runtime give us that for
;; free via the call stack.
;;
;; [1] If we used `recur` without saving the relevant rollback
;;     functions somewhere, we would loose their references and would
;;     not be able to call them later.
;;
;; There were two main reasons to go with plain recursion (as opposed
;; to use `recur` and manage the rollback functions stack ourselves):
;;
;; 1. The code is far easier to read and understand using regular
;;    recursion.
;;
;; 2. There is no realistic risk of blowing the call stack in this
;;    case. Because the recursion is limited to the number of entries
;;    in the initial `txns` collection argument to the
;;    `thread-transactions` call. Unless we have thousands of entries
;;    in that collection (very highly improbable, we usually have
;;    something in the order of 5 to 15 entries in there), we won't
;;    blow the stack *at all*.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn thread-transactions
  [logger txns args-map]
  {:pre [(m/validate transactions-schema txns)
         (m/validate args-map-schema args-map)]}
  (if-not (seq txns)
    ;; If there are no more transactions to process, then the passed
    ;; in `args-map` is the return value of the last transaction. If we
    ;; reached here is because the last transaction was successful. So
    ;; simply return `args-map` as the final value of the whole
    ;; transactions application.
    args-map
    (let [{:keys [txn-fn rollback-fn]} (first txns)
          result (safe-run logger txn-fn args-map)]
      (if-not (:success? result)
        result
        (let [next-result (thread-transactions logger (rest txns) result)]
          (if (:success? next-result)
            next-result
            (if-not rollback-fn
              next-result
              (safe-run logger rollback-fn next-result))))))))
