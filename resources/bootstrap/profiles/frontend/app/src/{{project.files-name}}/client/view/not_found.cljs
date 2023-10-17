;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.view.not-found
  (:require [<<project.name>>.client.view :as view]))

(def ^:const route-config
  {:name [:not-found]})

(defn- main []
  [:div
   [:span "Not found"]])

(defmethod view/view-display (:name route-config)
  [_]
  [main])
