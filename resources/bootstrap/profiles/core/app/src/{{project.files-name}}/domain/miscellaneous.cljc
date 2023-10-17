;; This Source Code Form is subject to the terms of the MIT license.
;; If a copy of the MIT license was not distributed with this
;; file, You can obtain one at https://opensource.org/licenses/MIT

{{=<< >>=}}
(ns <<project.name>>.domain.miscellaneous
  (:require [cljc.java-time.zone-id :as jt.zone-id]
            [clojure.string :as str]
            [malli.core :as m]))

(def email-schema
  (m/schema
   [:and
    [:string]
    [:re #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"]]))

(def hex-color-schema
  (m/schema
   [:and
    [:string
     {:min 7 :max 7
      :decode/string str/lower-case}]
    [:re #"^#[0-9a-f]{6}$"]]))

(def base64-schema
  (m/schema
   [:and
    [:string]
    [:or
     [:= ""]
     [:and
      [:re #"[0-9a-zA-Z+/]+={0,2}"]
      [:fn #(= 0 (rem (count %) 4))]]]]))

(def timezone-id-schema
  (m/schema
   [:and
    [:string]
    [:fn
     (fn [s]
       (try
         (some? (jt.zone-id/of s))
         (catch #?(:clj Throwable :cljs :default) _
           false)))]]))
