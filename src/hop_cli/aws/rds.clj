;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.rds
  (:require [babashka.process :refer [shell]]
            [hop-cli.aws.api.ec2 :as api.ec2]
            [hop-cli.aws.api.rds :as api.rds]
            [hop-cli.aws.api.resourcegroupstagging :as api.resourcegroupstagging]))

(defn- exec-port-forwarding-command
  [{:keys [id] :as _ec2-instance}
   {{:keys [host port]} :connection-details :as _rds-instance}
   local-port]
  (let [target-cmd (format "aws ssm start-session
                            --target '%s'
                            --document-name AWS-StartPortForwardingSessionToRemoteHost
                            --parameters '{\"host\":[\"%s\"],
                                           \"portNumber\":[\"%s\"],
                                           \"localPortNumber\":[\"%s\"]}'"
                           id host port local-port)]
    (println "Running AWS Session Manager. Please, press `ctrl+c` in order to cancel the process.")
    (try
      (shell target-cmd)
      {:success? true}
      (catch Exception e
        {:success? false
         :error-details (cond-> {:exception-message (.getMessage e)}
                          (seq (ex-data e))
                          (assoc :exit-code (:exit (ex-data e))))}))))

(defn- find-rds-instance
  [{:keys [project-name environment region]}]
  (let [search-args {:tags {:project-name project-name
                            :environment environment}
                     :resource-types ["rds:db"]
                     :region region}
        {:keys [success? resources] :as search-result}
        (api.resourcegroupstagging/get-resource-arns search-args)]
    (if (or (not success?) (empty? resources))
      {:success? false
       :reason :could-not-find-rds-instances
       :error-details search-result}
      (let [db-arn (-> resources first :arn)
            get-details-args {:arn db-arn :region region}
            {:keys [success? connection-details] :as get-details-result}
            (api.rds/get-instance-connection-details get-details-args)]
        (if-not success?
          {:success? false
           :reason :could-not-get-db-connection-details
           :error-details get-details-result}
          {:success? true
           :rds-instance {:connection-details connection-details}})))))

(defn- find-ec2-instance
  [{:keys [project-name environment region]}]
  (let [search-args {:filters {:tag:project-name project-name
                               :tag:environment environment
                               :instance-state-name "running"
                               :tag:elasticbeanstalk:environment-name
                               (str project-name "-" environment)}
                     :max-results 5 ;; It's the minimum
                     :region region}
        {:keys [success? instances] :as search-result}
        (api.ec2/describe-instances search-args)]
    (if (or (not success?) (= 0 (count instances)))
      {:success? false
       :reason :could-not-find-ec2-instance
       :error-details search-result}
      {:success? true
       :ec2-instance (first instances)})))

(defn start-port-forwarding-session
  [{:keys [local-port] :as opts}]
  (let [get-rds-instance-result (find-rds-instance opts)]
    (if-not (:success? get-rds-instance-result)
      get-rds-instance-result
      (let [get-ec2-instance-result (find-ec2-instance opts)]
        (if-not (:success? get-ec2-instance-result)
          get-ec2-instance-result
          (exec-port-forwarding-command
           (:ec2-instance get-ec2-instance-result)
           (:rds-instance get-rds-instance-result)
           local-port))))))
