(ns hop-cli.bootstrap.main
  (:require [hop-cli.bootstrap.infrastructure :as infrastructure]
            [hop-cli.bootstrap.infrastructure.aws]
            [hop-cli.bootstrap.profile :as profile]
            [hop-cli.bootstrap.settings-reader :as sr]
            [hop-cli.util.thread-transactions :as tht]))

(defn bootstrap-hop
  [{:keys [settings-file-path]}]
  (->
   [{:txn-fn
     (fn read-settings [_]
       (sr/read-settings settings-file-path))}
    {:txn-fn
     (fn provision-infrastructure
       [{:keys [settings]}]
       (let [result (infrastructure/provision-initial-infrastructure settings)]
         (if (:success? result)
           {:success? true
            :settings settings}
           {:success? false
            :reason :failed-to-provision-infrastructure
            :error-details result})))}
    {:txn-fn
     (fn generate-localhost-project
       [{:keys [settings]}]
       (let [result (profile/generate-project! settings)]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :could-not-generate-project
            :error-details result})))}
    {:txn-fn
     (fn save-environment-variables
       [{:keys [settings]}]
       (let [result (infrastructure/save-environment-variables settings)]
         (if (:success? result)
           result
           {:success? false
            :reason :could-not-save-env-variables
            :error-details result})))}]
   (tht/thread-transactions {})))
