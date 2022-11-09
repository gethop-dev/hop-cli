(ns <<project.name>>.api.main
  (:require [integrant.core :as ig]
            [malli.util :as mu]
            [muuntaja.core :as m]
            [reitit.coercion.malli :as coercion.malli]
            [reitit.coercion.spec]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as reitit.ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]))

(def router-config
  {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
   ;;:validate spec/validate ;; enable spec validation for route data
   ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
   :exception pretty/exception
   :data {:coercion (coercion.malli/create
                      {:error-keys #{:humanized}
                       :compile mu/closed-schema
                       :skip-extra-keys true
                       :default-values true
                       :encode-error (fn [error]
                                       {:success? false
                                        :reason :bad-parameter-type-or-format
                                        :error-details (:humanized error)})})
          :muuntaja m/instance
          :middleware [;; swagger feature
                       swagger/swagger-feature
                       ;; query-params & form-params
                       parameters/parameters-middleware
                       ;; content-negotiation
                       muuntaja/format-negotiate-middleware
                       ;; encoding response body
                       muuntaja/format-response-middleware
                       ;; exception handling
                       exception/exception-middleware
                       ;; decoding request body
                       muuntaja/format-request-middleware
                       ;; coercing response bodys
                       coercion/coerce-response-middleware
                       ;; coercing request parameters
                       coercion/coerce-request-middleware
                       ;; multipart
                       multipart/multipart-middleware]}})

(def swagger-docs
  ["/swagger.json"
   {:get
    {:no-doc true
     :swagger {:info {:title "<<project.name>> API reference"
                      :description "The <<project.name>> API"
                      :version "1.0.0"}}
     :handler (swagger/create-swagger-handler)}}])

(def ^:const api-context
  ["/api"])

(defn build-api-routes
  [routes]
  (reduce conj (conj api-context swagger-docs) routes))

(defmethod ig/init-key :reitit-demo.api/main
  [_ {:keys [routes]}]
  (let [root (first routes)
        api-routes (build-api-routes (rest routes))]
    (reitit.ring/ring-handler
     (reitit.ring/router
      [root
       api-routes]
      router-config)
     (reitit.ring/routes
      (swagger-ui/create-swagger-ui-handler {:path "/api/docs"
                                             :url "/api/swagger.json"
                                             :validatorUrl nil
                                             :apisSorter "alpha"
                                             :operationsSorter "alpha"})
      (reitit.ring/create-resource-handler {:path "/" :root "<<project.name>>/public"})
      (reitit.ring/create-default-handler)))))
