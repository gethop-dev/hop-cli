;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.middleware.nested-query-parameters
  (:require [malli.core :as m]
            [malli.util :as mu]
            [ring.middleware.nested-params :as nested-params]))

(def nested-query-parameters-middleware
  "Middleware that converts flat query-parameters into nested maps.

  It's only applied if the route query parameters schema includes nested maps."
  {:name ::nested-query-parameters
   :compile
   (fn [{:keys [parameters]} _opts]
     (when (and (:query parameters)
                (->> (mu/subschemas (:query parameters))
                     (filter #(= 1 (count (:path %))))
                     (some #(= :map (m/type (:schema %))))))
       (fn [handler]
         (fn [{:keys [query-params] :as request}]
           (if-not (seq query-params)
             (handler request)
             (let [{:keys [params]} (nested-params/nested-params-request {:params query-params})]
               (handler (assoc request :query-params params))))))))})
