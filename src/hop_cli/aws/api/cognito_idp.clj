(ns hop-cli.aws.api.cognito-idp
  (:require [com.grzm.awyeah.client.api :as aws]))

(defonce idp-client
  (aws/client {:api :cognito-idp}))

(defn- attributes->api-attributes
  [attributes]
  (map
   (fn [[k v]]
     {:Name (name k)
      :Value v})
   attributes))

(defn admin-create-user
  [{:keys [user-pool-id username attributes temporary-password]}]
  (let [api-attributes (attributes->api-attributes attributes)
        request {:UserPoolId user-pool-id
                 :Username username
                 :UserAttributes api-attributes
                 :TemporaryPassword temporary-password}
        opts {:op :AdminCreateUser
              :request request}
        result (aws/invoke idp-client opts)]
    (if-let [user (:User result)]
      {:success? true
       :user user}
      {:success? false
       :error-details result})))

(defn admin-set-user-password
  [{:keys [user-pool-id username password temporary?]}]
  (let [request {:UserPoolId user-pool-id
                 :Username username
                 :Password password
                 :Permanent (not temporary?)}
        opts {:op :AdminSetUserPassword
              :request request}
        result (aws/invoke idp-client opts)]
    (if (empty? result)
      {:success? true}
      {:success? false
       :error-details result})))

(defn get-tokens
  [{:keys [user-pool-id client-id username password]}]
  (let [request {:UserPoolId user-pool-id
                 :ClientId client-id
                 :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                 :AuthParameters {:USERNAME username
                                  :PASSWORD password}}
        opts {:op :AdminInitiateAuth
              :request request}
        result (aws/invoke idp-client opts)]
    (if (:AuthenticationResult result)
      {:success? true
       :tokens {:id-token (get-in result [:AuthenticationResult :IdToken])
                :access-token (get-in result [:AuthenticationResult :AccessToken])
                :refresh-token (get-in result [:AuthenticationResult :RefreshToken])}}
      {:success? false
       :error-details result})))
