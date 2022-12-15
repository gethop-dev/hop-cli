;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.localization
  (:require [<<project.name>>.shared.localization.dictionaries :as dictionaries]
            [re-frame.core :as rf]
            [taoensso.tempura :refer [tr]]))

(defn t*
  ([resource-id languages]
   (tr {:dict dictionaries/dictionaries} languages resource-id))
  ([resource-id languages arguments]
   (tr {:dict dictionaries/dictionaries} languages resource-id arguments)))

(rf/reg-sub
 ::language
 (fn [db _]
   (get db :language dictionaries/default-language)))

(rf/reg-event-db
 ::set-language
 (fn [db [_ language]]
   (assoc db :language language)))

(defn t
  ([resource-id] (t resource-id []))
  ([resource-id arguments]
   (let [language (rf/subscribe [::language])]
     (t* resource-id [@language] arguments))))
