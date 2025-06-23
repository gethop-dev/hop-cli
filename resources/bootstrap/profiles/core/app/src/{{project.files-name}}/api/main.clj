;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.main
  (:require [<<project.name>>.api.middleware.nested-query-parameters :as mid.nested-query-parameters]
            [<<project.name>>.api.muuntaja.instance :as muuntaja.instance]
            [<<project.name>>.shared.util.malli-coercion :as util.malli-coercion]
            [integrant.core :as ig]
            [reitit.coercion.spec]
            [reitit.ring :as reitit.ring]
            [reitit.ring.coercion :as mid.coercion]
            [reitit.ring.middleware.exception :as mid.exception]
            [reitit.ring.middleware.multipart :as mid.multipart]
            [reitit.ring.middleware.muuntaja :as mid.muuntaja]
            [reitit.ring.middleware.parameters :as mid.parameters]
            [reitit.ring.spec :as reitit.spec]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]))

(defn- build-router-config
  [_opts]
  {:validate reitit.spec/validate
   :reitit.ring/default-options-endpoint {:no-doc true
                                          :handler reitit.ring/default-options-handler}
   :data {:coercion util.malli-coercion/custom-reitit-malli-coercer
          :muuntaja (muuntaja.instance/build-muuntaja-instance)
          :middleware [;; swagger feature
                       swagger/swagger-feature
                       ;; content-negotiation
                       mid.muuntaja/format-negotiate-middleware
                       ;; encoding response body
                       mid.muuntaja/format-response-middleware
                       ;; exception handling
                       mid.exception/exception-middleware
                       ;; coercing response body
                       mid.coercion/coerce-response-middleware
                       ;; query-params & form-params
                       mid.parameters/parameters-middleware
                       ;; Flat query-params to nested
                       mid.nested-query-parameters/nested-query-parameters-middleware
                       ;; decoding request body
                       mid.muuntaja/format-request-middleware
                       ;; coercing request parameters
                       mid.coercion/coerce-request-middleware
                       ;; multipart
                       mid.multipart/multipart-middleware]}})

(defn- build-docs-routes
  [_opts]
  [["/docs"
    ["/ui/*"
     {:get
      {:no-doc true
       :handler (swagger-ui/create-swagger-ui-handler
                 {:url "/api/docs/specification/full-api.json"
                  :config {:validatorUrl nil}})}}]
    ["/specification"
     ["/full-api.json"
      {:get
       {:no-doc true
        :swagger {:info {:title "<<project.name>> API reference"}}
        :handler (swagger/create-swagger-handler)}}]]]])

(defn- build-router
  [{:keys [routes api-routes] :as opts}]
  (reitit.ring/router
   [routes
    ["/api"
     (build-docs-routes opts)
     api-routes]]
   (build-router-config opts)))

(defn- build-default-handler
  [_opts]
  (reitit.ring/routes
   (reitit.ring/create-resource-handler {:path "/" :root "<<project.files-name>>/public"})
   (reitit.ring/create-default-handler)))

(defmethod ig/init-key :<<project.name>>.api/main
  [_ opts]
  (reitit.ring/ring-handler
   (build-router opts)
   (build-default-handler opts)))
