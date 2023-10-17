;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.navigation
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitit.fre]))

(rf/reg-fx
 :do-push-state
 (fn [route]
   (apply reitit.fre/push-state route)))

(rf/reg-event-fx
 ::push-state
 (fn [_ [_ & route]]
   {:do-push-state route}))

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (reitit.fre/href k params query)))
