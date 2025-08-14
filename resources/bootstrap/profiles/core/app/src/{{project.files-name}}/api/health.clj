;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.health
  (:require [<<project.name>>.service.health :as srv.health]
            [integrant.core :as ig]))

(defn health-handler
  [config _req]
  (let [health (srv.health/get-health config)]
    {:status (if (:healthy? health) 200 503)
     :body health}))

(defmethod ig/init-key :<<project.name>>.api/health
  [_ config]
  ["/health"
   {:get {:summary "Return application's health"
          :authentication :none
          :openapi {:tags ["health"]}
          :handler (partial health-handler config)}}])
