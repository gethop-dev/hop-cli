;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.localization
  (:require [<<project.name>>.shared.localization.dictionaries :as dictionaries]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [taoensso.tempura :refer [tr]]))


(defn t*
  ([resource-id languages]
   (tr {:dict dictionaries/dictionaries} languages resource-id))
  ([resource-id languages arguments]
   (tr {:dict dictionaries/dictionaries} languages resource-id arguments)))

(defn get-language
  [db]
  (or (get db :session-language)
      (get-in db [:session :user :language])
      (get db :browser-language)
      dictionaries/default-language))

(rf/reg-sub
 ::language
 (fn [db _]
   (get-language db)))

(rf/reg-sub
 ::session-language
 (fn [db]
   (get db :session-language)))

(rf/reg-event-db
 ::set-session-language
 (fn [db [_ session-language]]
   (assoc db :session-language session-language)))

(rf/reg-event-fx
 ::set-browser-language
 [(rf/inject-cofx :browser-language)]
 (fn [{:keys [db browser-language]} _]
   {:db (assoc db :browser-language browser-language)}))

(rf/reg-cofx
 :browser-language
 (fn [cofx]
   (let [languages js/navigator.languages
         first-supported-language
         (some #(get dictionaries/languages
                     (keyword (first (str/split % #"-"))))
               languages)]
     (assoc cofx :browser-language first-supported-language))))

(defn t
  ([resource-id] (t resource-id []))
  ([resource-id arguments]
   (let [language (rf/subscribe [::language])]
     (t* resource-id [@language] arguments))))
