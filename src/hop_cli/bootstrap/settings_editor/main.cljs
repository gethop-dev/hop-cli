;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns main
  (:require [ajax.core :refer [GET]]
            [clojure.edn :as edn]
            [editor :as editor]
            [profile-picker :as profile-picker]
            [re-frame.core :as rf]
            [reagent.dom :as rdom]
            [settings :as settings]
            [sidebar]
            [view :as view]))

(def default-appdb
  {:active-view :profile-picker})

(rf/reg-fx
 :do-get-request
 (fn [{:keys [uri handler-evt]}]
   (GET uri {:handler (fn [response]
                        (rf/dispatch (conj handler-evt response)))})))

(rf/reg-event-fx
 ::default-settings-loaded
 (fn [_ [_ response]]
   {:fx [[:dispatch [::settings/set-settings (edn/read-string response)]]]}))

(rf/reg-event-fx
 ::load-default-settings
 (fn [_]
   {:do-get-request {:uri "/settings.edn"
                     :handler-evt [::default-settings-loaded]}}))

(rf/reg-event-fx
 ::load-app
 (fn [_]
   {:db default-appdb
    :fx [[:dispatch [::load-default-settings]]]}))

(rf/dispatch-sync [::load-app])

(defn- navigation
  []
  [:div.navigation
   [:img.navigation__logo
    {:src "img/hop-logo-small.svg"}]
   [:a.navigation__link
    {:href "https://gethop.dev"
     :target "_blank"}
    [:img
     {:src "img/open-url.svg"}]]])

(defn root-component []
  (let [active-view (rf/subscribe [::view/active-view])]
    (fn []
      [:div.settings-editor
       [navigation]
       (case @active-view
         :editor [editor/main]
         :profile-picker [profile-picker/main]
         [:div])])))

(rdom/render [root-component]
             (.getElementById js/document "app"))
