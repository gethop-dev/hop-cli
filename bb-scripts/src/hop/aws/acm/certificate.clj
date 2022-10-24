(ns hop.aws.acm.certificate
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
