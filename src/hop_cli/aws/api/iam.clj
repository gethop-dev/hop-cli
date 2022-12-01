(ns hop-cli.aws.api.iam
  (:require [com.grzm.awyeah.client.api :as aws]))

(defonce iam-client
  (aws/client {:api :iam}))

(defn create-access-key
  [{:keys [username]}]
  (let [request {:UserName username}
        opts {:op :CreateAccessKey
              :request request}
        result (aws/invoke iam-client opts)]
    (if-let [access-key (:AccessKey result)]
      {:success? true
       :access-key {:id (:AccessKeyId access-key)
                    :secret (:SecretAccessKey access-key)}}
      {:success? false
       :reason result})))
