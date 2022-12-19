;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.sts
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
