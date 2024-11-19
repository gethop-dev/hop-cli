;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.eb
  (:require [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn update-env-variable
  [{:keys [project-name environment name value] :as opts}]
  (let [eb-client (aws.client/gen-client :elasticbeanstalk opts)
        request {:EnvironmentName (str project-name "-" environment)
                 :OptionSettings [{:Namespace "aws:elasticbeanstalk:application:environment"
                                   :OptionName name
                                   :Value value}]}
        args {:op :UpdateEnvironment
              :request request}
        result (aws/invoke eb-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :result result})))

(defn get-latest-eb-docker-platform-arn
  [opts]
  (let [eb-client (aws.client/gen-client :elasticbeanstalk opts)
        request {:Filters [{:Type "PlatformBranchName"
                            :Operator "="
                            :Values ["Docker running on 64bit Amazon Linux 2"]}]}
        args {:op :ListPlatformVersions
              :request request}
        result (aws/invoke eb-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      (if-let [platform-arn (->> (:PlatformSummaryList result)
                                 (sort-by :PlatformVersion (complement comp))
                                 (first)
                                 (:PlatformArn))]
        {:success? true
         :platform-arn platform-arn}
        {:success? false
         :reason :docker-platform-arn-not-found
         :error-details result}))))
