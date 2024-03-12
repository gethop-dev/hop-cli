;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util.http
  (:require [cheshire.core :as json]
            [org.httpkit.client :as http]))

(defn- encode-request
  [request]
  (try
    {:success? true
     :request
     (cond-> request
       (= "application/json" (get-in request [:headers "Content-Type"]))
       (update :body json/generate-string))}
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
