(ns hop-cli.aws.cognito
  (:require [hop-cli.aws.api.cognito-idp :as api.cognito-idp]))

(defn admin-create-user
  [opts]
  (api.cognito-idp/admin-create-user opts))

(defn admin-set-user-password
  [opts]
  (api.cognito-idp/admin-set-user-password opts))

(defn get-id-token
  [opts]
  (let [result (api.cognito-idp/get-tokens opts)]
    (if (:success? result)
      {:success? true
       :id-token (get-in result [:tokens :id-token])}
      result)))
