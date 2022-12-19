;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.acm
  (:require [com.grzm.awyeah.client.api :as aws]))

(defonce acm-client
  (aws/client {:api :acm}))

(defn import-certificate
  [{:keys [certificate private-key]}]
  (let [request {:Certificate certificate
                 :PrivateKey private-key}
        opts {:op :ImportCertificate
              :request request}
        result (aws/invoke acm-client opts)]
    (if-let [certificate-arn (:CertificateArn result)]
      {:success? true
       :certificate-arn certificate-arn}
      {:success? false
       :error-details result})))
