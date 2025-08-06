;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.rds
  (:require [cognitect.aws.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn describe-instance
  [{:keys [arn] :as opts}]
  (let [client (aws.client/gen-client :rds opts)
        request {:DBInstanceIdentifier arn}
        args {:op :DescribeDBInstances
              :request request}
        result (aws/invoke client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :instance (-> result :DBInstances first)})))

(defn get-instance-connection-details
  [opts]
  (let [{:keys [success? instance] :as result} (describe-instance opts)]
    (if success?
      {:success? true
       :connection-details {:host (get-in instance [:Endpoint :Address])
                            :port (get-in instance [:Endpoint :Port])}}
      result)))
