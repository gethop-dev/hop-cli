;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.acm
  (:require [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn import-certificate
  [{:keys [certificate private-key] :as opts}]
  (let [acm-client (aws.client/gen-client :acm opts)
        request {:Certificate certificate
                 :PrivateKey private-key}
        args {:op :ImportCertificate
              :request request}
        result (aws/invoke acm-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :certificate-arn (:CertificateArn result)})))
