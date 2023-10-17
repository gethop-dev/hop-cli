;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.transit
  (:require [cljc.java-time.format.date-time-formatter :as jt.formatter]
            [cljc.java-time.instant :as jt.instant]
            [cognitect.transit :as transit])
  #?(:clj (:import [java.time Instant])))

(def time-formatter jt.formatter/iso-instant)

(def instant-write-handler
  (transit/write-handler
   (constantly "biotz/Instant")
   (fn [instant]
     (jt.formatter/format time-formatter instant))))

(def instant-read-handler
  (transit/read-handler
   (fn [iso-string]
     (->> (jt.formatter/parse time-formatter iso-string)
          (jt.instant/from)))))

(def custom-write-handlers
  {#?(:clj Instant :cljs js/Date)
   instant-write-handler})

(def custom-read-handlers
  {"biotz/Instant"
   instant-read-handler})
