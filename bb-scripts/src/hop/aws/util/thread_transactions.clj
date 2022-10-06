(ns hop.aws.util.thread-transactions)

(defn- safe-run [f m]
  (try
    (f m)
    (catch Throwable e
      (merge m {:success? false
                :error-details {:reason (class e)
                                :message (.getMessage e)}}))))

(defn thread-transactions
  [txns args-map]
  (if-not (seq txns)
    ;; If there are no more transactions to process, then the passed
    ;; in `args-map` is the return value of the last transaction. If we
    ;; reached here is because the last transaction was successful. So
    ;; simply return `args-map` as the final value of the whole
    ;; transactions application.
    args-map
    (let [{:keys [txn-fn rollback-fn]} (first txns)
          result (safe-run txn-fn args-map)]
      (if-not (:success? result)
        result
        (let [next-result (thread-transactions (rest txns) result)]
          (if (:success? next-result)
            next-result
            (if-not rollback-fn
              next-result
              (safe-run rollback-fn next-result))))))))
