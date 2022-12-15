(ns hop-cli.keycloak.api.openid-connect
  (:require [clojure.set :as set]
            [hop-cli.util.http :as util.http]))

(defn get-token
  [{:keys [base-url username password realm-name client-id]}]
  (let [url (format "%s/realms/%s/protocol/openid-connect/token" base-url realm-name)
        params {:username username
                :password password
                :grant_type "password"
                :client_id client-id
                :scope "openid"}
        request {:method :post
                 :url url
                 :form-params params}
        result (util.http/make-request request)]
    (if (:success? result)
      {:success? true
       :token (get-in result [:response :body])}
      {:success? false
       :error-details result})))

(defn get-id-token
  [opts]
  (let [result (get-token opts)]
    (if (:success? result)
      {:success? true
       :id-token (-> result :token :id_token)}
      result)))

(defn get-admin-access-token
  [opts]
  (let [opts (set/rename-keys opts {:admin-username :username
                                    :admin-password :password
                                    :admin-client-id :client-id
                                    :admin-realm-name :realm-name})
        result (get-token opts)]
    (if (:success? result)
      {:success? true
       :access-token (-> result :token :access_token)}
      result)))
