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
      (let [response @(http/request (:request encode-result))]
        (if-not (<= 200 (:status response) 299)
          {:success? false
           :error-details response}
          (decode-response response))))))
