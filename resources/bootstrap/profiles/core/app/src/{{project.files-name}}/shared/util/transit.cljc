;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.transit
  (:require [cljc.java-time.format.date-time-formatter :as jt.formatter]
            [cljc.java-time.instant :as jt.instant]
            [cognitect.transit :as transit]
            #?(:clj [clojure.java.io :as io]))
  #?(:clj (:import [java.io ByteArrayOutputStream]
                   [java.time Instant])))

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
