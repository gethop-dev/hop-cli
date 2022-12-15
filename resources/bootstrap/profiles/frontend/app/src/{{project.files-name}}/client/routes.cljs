;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.routes
  (:require [<<project.name>>.client.landing :as landing]
            [<<project.name>>.shared.util.routing :as util.routing]
            [re-frame.core :as rf]
            [reitit.frontend :as reitit.fr]
            [reitit.frontend.controllers :as reitit.frc]
            [reitit.frontend.easy :as reitit.fre]))

(rf/reg-fx
 :push-state
 (fn [route]
   (apply reitit.fre/push-state route)))

(rf/reg-event-fx
 ::push-state
 (fn [_ [_ & route]]
   {:push-state route}))

(rf/reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (let [old-match   (:current-route db)
         controllers (reitit.frc/apply-controllers (:controllers old-match) new-match)]
     (assoc db :current-route (assoc new-match :controllers controllers)))))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (reitit.fre/href k params query)))

(def routes
  ["/"
   ["landing"
    {:name ::landing/view
     :controllers [{:start (fn [& _params] (js/console.log "Entering Landing"))
                    :stop  (fn [& _params] (js/console.log "Leaving Landing"))}]}]])

(defn on-navigate [new-match]
  (when new-match
    (rf/dispatch [::navigated new-match])))

(def router
  (reitit.fr/router
   routes
   {:data {:coercion util.routing/custom-malli-coercion}}))

(defn init-routes! []
  (js/console.log "initializing routes")
  (reitit.fre/start!
   router
   on-navigate
   {:use-fragment true                                      ;; using "true" for easier route debugging without SSR.
    :on-coercion-error (fn [_ _] (js/alert "Coercion error!"))})
  (rf/dispatch [::push-state ::landing/view]))
