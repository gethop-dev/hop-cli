(ns hop-cli.aws.sts.identity
  (:require [com.grzm.awyeah.client.api :as aws]))

(defonce sts-client
         (aws/client {:api :sts}))

(defn get-caller-identity
  []
  (let [request {:op :GetCallerIdentity
                 :request {}}
        result (aws/invoke sts-client request)]
    (if (:category result)
      {:success? false
       :error-details {:result result}}
      {:success? true
       :caller-identity result})))
