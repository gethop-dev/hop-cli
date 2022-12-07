(ns hop-cli.keycloak.api.user
  (:require [hop-cli.util.http :as util.http]))

(defn create-user
  [{:keys [base-url access-token realm-name username temporary-password
           first-name last-name attributes email email-verified]}]
  (let [url (format "%s/admin/realms/%s/users" base-url realm-name)
        user (cond-> {:enabled true
                      :username username}
               temporary-password
               (assoc :credentials [{:type "password"
                                     :value temporary-password
                                     :temporary true}])
               (seq attributes)
               (assoc :attributes attributes)

               email
               (assoc :email email
                      :emailVerified (boolean email-verified))

               first-name
               (assoc :firstName first-name)

               last-name
               (assoc :lastName last-name))
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " access-token)}
        request {:url url
                 :method :post
                 :headers headers
                 :body user}
        result (util.http/make-request request)]
    (if (:success? result)
      {:success? true}
      result)))

(defn get-user
  [{:keys [base-url access-token realm-name username]}]
  (let [url (format "%s/admin/realms/%s/users" base-url realm-name)
        opts {:exact true
              :username username}
        headers {"Authorization" (str "Bearer " access-token)}
        request {:url url
                 :method :get
                 :headers headers
                 :query-params opts}
        result (util.http/make-request request)]
    (if (:success? result)
      (if (= 1 (count (get-in result [:response :body])))
        {:success? true
         :user (first (get-in result [:response :body]))}
        {:success? false
         :reason :multiple-or-none-users-found
         :error-details {:result result}})
      result)))

(defn set-user-password
  [{:keys [base-url access-token realm-name user-id password temporary?]}]
  (let [url (format "%s/admin/realms/%s/users/%s/reset-password"
                    base-url realm-name user-id)
        cred {:type "password"
              :value password
              :temporary (boolean temporary?)}
        headers {"Content-Type" "application/json"
                 "Authorization" (str "Bearer " access-token)}
        request {:url url
                 :method :put
                 :headers headers
                 :body cred}
        result (util.http/make-request request)]
    (if (:success? result)
      {:success? true}
      result)))
