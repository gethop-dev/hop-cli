;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.cognito
  (:require [hop-cli.aws.api.cognito-idp :as api.cognito-idp]
            [taoensso.timbre :as timbre]))

(defn admin-create-user
  [opts]
  (api.cognito-idp/admin-create-user opts))

(defn admin-set-user-password
  [opts]
  (api.cognito-idp/admin-set-user-password opts))

(defn get-id-token
  [opts]
  (timbre/with-level :warn
    (let [result (api.cognito-idp/get-tokens opts)]
      (if (:success? result)
        (if (:raw opts)
          (get-in result [:tokens :id-token])
          {:success? true
           :id-token (get-in result [:tokens :id-token])})
        result))))
