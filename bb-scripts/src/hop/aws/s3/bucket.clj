(ns hop.aws.s3.bucket
  (:require [com.grzm.awyeah.client.api :as aws]))

(defonce s3-client
         (aws/client {:api :s3}))

(defn head-bucket
  [_config {:keys [bucket-name]}]
  (let [request {:Bucket bucket-name}
        opts {:op :HeadBucket
              :request request}
        result (aws/invoke s3-client opts)]
    (if (= {} result)
      {:success? true}
      {:success? false
       :error-details result})))

(defn create-bucket
  [{:keys [region]} {:keys [bucket-name]}]
  (let [request {:Bucket bucket-name
                 :CreateBucketConfiguration {:LocationConstraint region}}
        opts {:op :CreateBucket
              :request request}
        result (aws/invoke s3-client opts)]
    (if (:Error result)
      {:success? false
       :error-details result}
      {:success? true})))

(defn delete-bucket
  [_config bucket-name]
  (let [request {:Bucket bucket-name}
        opts {:op :DeleteBucket
              :request request}
        result (aws/invoke s3-client opts)]
    (if (:Error result)
      {:success? false
       :error-details result}
      {:success? true})))

(defn put-object
  [_config {:keys [bucket-name key body]}]
  (let [request {:Bucket bucket-name
                 :Key key
                 :Body body}
        opts {:op :PutObject
              :request request}
        result (aws/invoke s3-client opts)]
    (if (:category result)
      {:success? false
       :error-details {:result result}}
      {:success? true})))
