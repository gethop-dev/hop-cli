;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.rds
  (:require [babashka.process :refer [shell]]
            [clojure.string :as str]
            [hop-cli.aws.api.rds :as api.rds]
            [hop-cli.aws.api.resourcegroupstagging :as api.resourcegroupstagging]))

(defn- exec-port-forwarding-command
  [ec2-id {:keys [host port]} local-port]
  (let [target-cmd (format "aws ssm start-session --target %s --document-name AWS-StartPortForwardingSessionToRemoteHost --parameters '{\"host\":[\"%s\"],\"portNumber\":[\"%s\"], \"localPortNumber\":[\"%s\"]}'"
                           ec2-id host port local-port)]
    (println "Running AWS Session Manager. Please, press `ctrl+c` in order to cancel the process.")
    (shell target-cmd)))

(defn start-port-forwarding-session
  [{:keys [project-name environment local-port region]}]
  (let [args {:tags {:project-name project-name
                     :environment environment}
              :resource-types ["ec2:instance" "rds:db"]
              :region region}
        {:keys [success? resource-arns] :as get-arns-result}
        (api.resourcegroupstagging/get-resource-arns args)]
    (if (and success? (<= 2 (count resource-arns)))
      (let [db-arn (first (filter #(str/starts-with? % "arn:aws:rds") resource-arns))
            ec2-arn (first (filter #(str/starts-with? % "arn:aws:ec2") resource-arns))
            ec2-id (second (re-find #":instance/(.*)$" ec2-arn))]
        (if (and db-arn ec2-arn ec2-id)
          (let [{:keys [success? connection-details] :as get-rds-result}
                (api.rds/get-instance-connection-details {:arn db-arn :region region})]
            (if success?
              {:success? true
               :command (exec-port-forwarding-command ec2-id connection-details local-port)}
              {:success? false
               :reason :could-not-find-rds-connection-details
               :error-details {:arn db-arn :result get-rds-result}}))
          {:success? false
           :reason :could-not-find-arns
           :error-details get-arns-result}))
      {:success? false
       :reason :could-not-get-arns
       :error-details get-arns-result})))
