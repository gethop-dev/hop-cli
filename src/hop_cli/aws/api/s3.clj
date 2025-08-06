;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.s3
  (:require [cognitect.aws.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn head-bucket
  [{:keys [bucket-name] :as opts}]
  (let [s3-client (aws.client/gen-client :s3 opts)
        request {:Bucket bucket-name}
        args {:op :HeadBucket
              :request request}
        result (aws/invoke s3-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true})))

(defn create-bucket
  [{:keys [bucket-name region] :as opts}]
  (let [s3-client (aws.client/gen-client :s3 opts)
        request {:Bucket bucket-name
                 :CreateBucketConfiguration {:LocationConstraint region}}
        args {:op :CreateBucket
              :request request}
        result (aws/invoke s3-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true})))

(defn delete-bucket
  [{:keys [bucket-name] :as opts}]
  (let [s3-client (aws.client/gen-client :s3 opts)
        request {:Bucket bucket-name}
        args {:op :DeleteBucket
              :request request}
        result (aws/invoke s3-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true})))

(defn put-object
  [{:keys [bucket-name key body] :as opts}]
  (let [s3-client (aws.client/gen-client :s3 opts)
        request {:Bucket bucket-name
                 :Key key
                 :Body body}
        args {:op :PutObject
              :request request}
        result (aws/invoke s3-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details {:result result}}
      {:success? true})))
