;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util.http
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [org.httpkit.client :as http])
  (:import (java.security KeyStore)
           (java.security.cert CertificateFactory X509Certificate)
           (javax.net.ssl SSLContext)
           (javax.net.ssl SSLContext TrustManagerFactory)))

(defn- custom-ssl-engine
  [cacert]
  (let [ca-is (-> cacert
                  (io/file)
                  (io/input-stream))
        ca-certs (-> (CertificateFactory/getInstance "x509")
                     (.generateCertificates ca-is))
        keystore (doto (KeyStore/getInstance (KeyStore/getDefaultType))
                   (.load nil nil))
        _ (dorun (map (fn [^X509Certificate cert counter]
                        (.setCertificateEntry keystore
                                              (str "cacert-alias-" counter)
                                              cert))
                      ca-certs
                      (range)))
        default-alg (TrustManagerFactory/getDefaultAlgorithm)
        trustmanager-factory (doto (TrustManagerFactory/getInstance default-alg)
                               (.init keystore))
        trustmanagers (.getTrustManagers trustmanager-factory)
        sslcontext (doto (SSLContext/getInstance "TLS")
                     (.init nil trustmanagers nil))]
    (http/make-ssl-engine sslcontext)))

(defn- encode-request
  [{:keys [cacert] :as request}]
  (try
    {:success? true
     :request
     (cond-> request
       (= "application/json" (get-in request [:headers "Content-Type"]))
       (update :body json/generate-string)

       cacert
       (-> (assoc :sslengine (custom-ssl-engine cacert))
           (dissoc :cacert)))}
    (catch Exception e
      {:success? false
       :reason :could-not-encode-request
       :error-details {:request request :exception e}})))

(defn- decode-response
  [response]
  (try
    {:success? true
     :response
     (cond-> response
       (= "application/json" (get-in response [:headers :content-type]))
       (update :body json/parse-string true))}
    (catch Exception e
      {:success? false
       :reason :could-not-parse-response
       :error-details {:response response :exception e}})))

(defn make-request
  [request]
  (let [encode-result (encode-request request)]
    (if-not (:success? encode-result)
      encode-result
      (let [{:keys [status] :as response}
            @(http/request (:request encode-result))]
        (if-not (and status (<= 200 status 299))
          {:success? false
           :error-details response}
          (decode-response response))))))
