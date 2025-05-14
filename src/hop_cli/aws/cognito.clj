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
  ;; timbre/with-level is deprecated upstream (one should use
  ;; timbre/with-min-level), but babashka doesn't expose it. It only
  ;; exposes timbre/with-level. Thus, we need to tell clj-kondo not to
  ;; complain about it in this case.
  #_{:clj-kondo/ignore [:deprecated-var]}
  (timbre/with-level :warn
    (let [result (api.cognito-idp/get-tokens opts)]
      (if (:success? result)
        (if (:raw opts)
          (get-in result [:tokens :id-token])
          {:success? true
           :id-token (get-in result [:tokens :id-token])})
        result))))
