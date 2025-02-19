;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.transit
  (:require [cljc.java-time.format.date-time-formatter :as jt.formatter]
            [cljc.java-time.instant :as jt.instant]
            [cljc.java-time.local-date :as jt.local-date]
            [cognitect.transit :as transit]
            #?(:clj [clojure.java.io :as io]))
  #?(:clj (:import [java.io ByteArrayOutputStream]
                   [java.time Instant LocalDate])))

(def instant-write-handler
  (transit/write-handler
   (constantly "hop/Instant")
   (fn [instant]
     (jt.formatter/format jt.formatter/iso-instant instant))))

(def instant-read-handler
  (transit/read-handler
   (fn [iso-string]
     (->> (jt.formatter/parse jt.formatter/iso-instant iso-string)
          (jt.instant/from)))))

(def local-date-write-handler
  (transit/write-handler
   (constantly "hop/LocalDate")
   (fn [local-date]
     (jt.formatter/format jt.formatter/iso-local-date local-date))))

(def local-date-read-handler
  (transit/read-handler
   (fn [iso-string]
     (->> (jt.formatter/parse jt.formatter/iso-local-date iso-string)
          (jt.local-date/from)))))

(def custom-write-handlers
  {#?(:clj Instant :cljs js/java.time.Instant)
   instant-write-handler
   #?(:clj LocalDate :cljs js/java.time.LocalDate)
   local-date-write-handler})

(def custom-read-handlers
  {"hop/Instant" instant-read-handler
   "hop/LocalDate" local-date-read-handler})

(defn encode-transit-json
  [x]
  (let [writer-opts {:handlers custom-write-handlers}]
    #?(:clj
       (with-open [out (ByteArrayOutputStream.)]
         (let [writer (transit/writer out :json writer-opts)]
           (transit/write writer x)
           (.toByteArray out)))
       :cljs
       (let [writer (transit/writer :json writer-opts)]
         (transit/write writer x)))))

(defn decode-transit-json
  [x]
  (let [reader-opts {:handlers custom-read-handlers}]
    (try
      #?(:clj
         (with-open [in (io/input-stream x)]
           (let [reader (transit/reader in :json reader-opts)]
             (transit/read reader)))
         :cljs
         (let [reader (transit/reader :json reader-opts)]
           (transit/read reader x)))
      (catch #?(:clj Exception, :cljs js/Error) _ nil))))
