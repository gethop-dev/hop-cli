;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.cognito-idp
  (:require [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn- attributes->api-attributes
  [attributes]
  (map
   (fn [[k v]]
     {:Name (name k)
      :Value v})
   attributes))

(defn admin-create-user
  [{:keys [user-pool-id username attributes temporary-password] :as opts}]
  (let [idp-client (aws.client/gen-client :cognito-idp opts)
        api-attributes (attributes->api-attributes attributes)
        request {:UserPoolId user-pool-id
                 :Username username
                 :UserAttributes api-attributes
                 :TemporaryPassword temporary-password}
        args {:op :AdminCreateUser
              :request request}
        result (aws/invoke idp-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :user (:User result)})))

(defn admin-set-user-password
  [{:keys [user-pool-id username password temporary?] :as opts}]
  (let [idp-client (aws.client/gen-client :cognito-idp opts)
        request {:UserPoolId user-pool-id
                 :Username username
                 :Password password
                 :Permanent (not temporary?)}
        args {:op :AdminSetUserPassword
              :request request}
        result (aws/invoke idp-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true})))

(defn get-tokens
  [{:keys [user-pool-id client-id username password] :as opts}]
  (let [idp-client (aws.client/gen-client :cognito-idp opts)
        request {:UserPoolId user-pool-id
                 :ClientId client-id
                 :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                 :AuthParameters {:USERNAME username
                                  :PASSWORD password}}
        args {:op :AdminInitiateAuth
              :request request}
        result (aws/invoke idp-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      (if (:ChallengeName result)
        {:success? false
         :error-details result}
        {:success? true
         :tokens {:id-token (get-in result [:AuthenticationResult :IdToken])
                  :access-token (get-in result [:AuthenticationResult :AccessToken])
                  :refresh-token (get-in result [:AuthenticationResult :RefreshToken])}}))))
