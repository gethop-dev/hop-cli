;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.iam
  (:require [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn create-access-key
  [{:keys [username] :as opts}]
  (let [iam-client (aws.client/gen-client :iam opts)
        request {:UserName username}
        args {:op :CreateAccessKey
              :request request}
        result (aws/invoke iam-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :reason result}
      {:success? true
       :access-key {:access-key-id (-> result :AccessKey :AccessKeyId)
                    :secret-access-key (-> result :AccessKey :SecretAccessKey)}})))
