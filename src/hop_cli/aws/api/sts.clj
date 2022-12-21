;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.sts
  (:require [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn get-caller-identity
  [opts]
  (let [sts-client (aws.client/gen-client :sts opts)
        request {:op :GetCallerIdentity
                 :request {}}
        result (aws/invoke sts-client request)]
    (if (:category result)
      {:success? false
       :error-details {:result result}}
      {:success? true
       :caller-identity result})))
