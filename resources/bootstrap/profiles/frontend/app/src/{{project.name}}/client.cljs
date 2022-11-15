;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client
  (:require [<<project.name>>.client.config :as client.config]
            [<<project.name>>.client.routes :as routes]
            [<<project.name>>.client.view :as view]
            [re-frame.core :as rf]
            [reagent.dom :as rd]
            <<#project.load-frontend-app.requires>><<&.>><</project.load-frontend-app.requires>>))

<<#project.load-frontend-app.code>>
<<&.>>
<</project.load-frontend-app.code>>

(rf/reg-event-fx
 ::on-config-loaded
 (fn [_ _]
   {:fx [<<#project.load-frontend-app.events>>
         <<&.>>
         <</project.load-frontend-app.events>>]}))

(rf/reg-event-fx
 ::load-app
 (fn [_ _]
   {:db {}
    :dispatch [[::client.config/load-config [::on-config-loaded]]]}))

(defn app []
  [:div.app-container
   [:div.app-container__main
    {:id "app-container__main"}
    [view/main]]])

(defn main []
  [app])

;; Make log level logs no-ops for production environment.
(rf/set-loggers! {:log (fn [& _])})

(defn dev-setup []
  (when goog.DEBUG
    ;; Reenable log level logs no-ops for dev environment.
    (rf/set-loggers! {:log js/console.log})
    (enable-console-print!)
    (println "Dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (rd/render [main] (.getElementById js/document "app")))

(defn ^:after-load re-render []
  (mount-root))

(defn ^:export init []
  (dev-setup)
  (rf/dispatch-sync [::load-app])
  (routes/init-routes!)
  (mount-root))
