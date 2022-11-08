(ns hop-cli.bootstrap.main
  (:require [clojure.walk :as walk]
            [hop-cli.aws.api.ssm :as api.ssm]
            [hop-cli.bootstrap.infrastructure.aws :as aws]
            [hop-cli.bootstrap.settings-reader :as sr]
            [hop-cli.bootstrap.template :as template]
            [hop-cli.util.thread-transactions :as tht]))

(defn bootstrap-hop
  [{:keys [settings-file-path target-project-dir]}]
  (->
   [{:txn-fn
     (fn read-settings [_]
       (sr/read-settings settings-file-path))}
    {:txn-fn
       (fn provision-infrastructure [{:keys [settings]}]
         (let [result (aws/provision-initial-infrastructure settings)]
           (if (:success? result)
             {:success? true
              :settings settings}
             {:success? false
              :reason :failed-to-provision-infrastructure
              :error-details result})))}
    {:txn-fn
     (fn generate-localhost-project [{:keys [settings]}]
       {:success? true
        :settings settings
        :project (template/foo (assoc settings :target-project-dir target-project-dir))})}
    {:txn-fn
     (fn upload-environment-variables-to-ssm
       [{:keys [settings project]}]
       (let [{:keys [environment-variables]} project
             config {:project-name (:project/name settings)
                     :environment "test"
                     :kms-key-alias (:aws.environment.test.kms/key-alias settings)}
             ssm-env-vars (->> environment-variables
                               :test
                               walk/stringify-keys
                               (map zipmap (repeat [:name :value])))
             result (api.ssm/put-parameters config {:new? true} ssm-env-vars)]
         (if (:success? result)
           result
           {:success? false
            :error-details result})))}]
   (tht/thread-transactions {})))
