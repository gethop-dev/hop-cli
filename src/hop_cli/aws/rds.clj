;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.rds
  (:require [clojure.string :as str]
            [hop-cli.aws.api.rds :as api.rds]
            [hop-cli.aws.api.resourcegroupstagging :as api.resourcegroupstagging]))

(defn- build-port-forwarding-command
  [ec2-id {:keys [host port]} local-port]
  (with-out-str
    (println "Run the following command to open a port forwarding tunnel to the RDS instance:")
    (println)
    (println
     (format "aws ssm start-session \\
 --target %s \\
 --document-name AWS-StartPortForwardingSessionToRemoteHost \\
 --parameters '{\"host\":[\"%s\"],
                \"portNumber\":[\"%s\"], \"localPortNumber\":[\"%s\"]}'"
             ec2-id host port local-port))
    (println)
    (println "Note: To run the command the AWS CLI Session Manager plugin is required:")
    (println "https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html")))

(defn get-port-forwarding-command
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
               :command (build-port-forwarding-command ec2-id connection-details local-port)}
              {:success? false
               :reason :could-not-find-rds-connection-details
               :error-details {:arn db-arn :result get-rds-result}}))
          {:success? false
           :reason :could-not-find-arns
           :error-details get-arns-result}))
      {:success? false
       :reason :could-not-get-arns
       :error-details get-arns-result})))
