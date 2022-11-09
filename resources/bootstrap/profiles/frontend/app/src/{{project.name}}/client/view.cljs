;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.view
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::current-route
 (fn [db]
   (:current-route db)))

(defmulti view-display #(if (vector? %) (first %) %))

(defmethod view-display :default
  [_]
  [:div "No content :("])

(defn main
  []
  (let [current-route (re-frame/subscribe [::current-route])]
    (fn []
      [view-display (get-in @current-route [:data :name])])))
