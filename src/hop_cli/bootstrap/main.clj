(ns hop-cli.bootstrap.main
  (:require [hop-cli.bootstrap.infrastructure.aws :as aws]
            [hop-cli.bootstrap.settings-reader :as sr]
            [hop-cli.util.thread-transactions :as tht]))

(defn bootstrap-hop
  [settings-file-path]
  (->
   [{:txn-fn
     (fn read-settings [_]
       (sr/read-settings settings-file-path))}
    {:txn-fn
     (fn provision-infrastructure [{:keys [settings]}]
       (let [result (aws/provision-infrastructure settings)]
         (if (:success? result)
           {:success? true
            :settings settings}
           {:success? false
            :reason :failed-to-provision-infrastructure
            :error-details result})))}]
   (tht/thread-transactions {})))
