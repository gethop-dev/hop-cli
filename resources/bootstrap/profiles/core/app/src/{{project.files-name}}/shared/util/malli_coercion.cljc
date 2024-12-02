;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.malli-coercion
  (:require [<<project.name>>.shared.util.json :as util.json]
            [<<project.name>>.shared.util.string :as util.string]
            [cljc.java-time.format.date-time-formatter :as jt.formatter]
            [cljc.java-time.format.date-time-formatter-builder :as jt.formatter-builder]
            [cljc.java-time.instant :as jt.instant]
            [cljc.java-time.local-date :as jt.local-date]
            [cljc.java-time.temporal.chrono-field :as jt.chrono-field]
            [clojure.string :as str]
            [malli.transform :as mt]
            [malli.util :as mu]
            [reitit.coercion.malli :as rcm]))

(defn- string->vector
  [x]
  (cond-> x
    (string? x)
    (str/split #",")))

(defn- vector->string
  [x]
  (cond->> x
    (vector? x)
    (str/join ",")))

(defn- string->tuple
  [x]
  (cond-> x
    (string? x)
    (str/split #"_")))

(defn- tuple->string
  [x]
  (cond->> x
    (vector? x)
    (str/join "_")))

(defn- string->normalized-string
  [x]
  (cond-> x
    (string? x)
    (util.string/->nfc-normalized-unicode)))

(defn json-string->coll
  [x]
  (try
    (cond-> x
      (string? x)
      (util.json/<-json))
    (catch #?(:clj Throwable :cljs :default) _
      x)))

(def instant-parse-formatter
  (-> #?(:clj (java.time.format.DateTimeFormatterBuilder.)
         :cljs (js/java.time.format.DateTimeFormatterBuilder.))
      (jt.formatter-builder/append-pattern "yyyy-MM-dd['T'HH:mm:ss]")

      (jt.formatter-builder/optional-start)
      (jt.formatter-builder/append-fraction jt.chrono-field/nano-of-second 0 9 true)
      (jt.formatter-builder/optional-end)

      (jt.formatter-builder/optional-start)
      (jt.formatter-builder/append-offset "+HHMMss" "Z")
      (jt.formatter-builder/optional-end)

      (jt.formatter-builder/optional-start)
      (jt.formatter-builder/append-offset "+HH:MM:ss" "Z")
      (jt.formatter-builder/optional-end)

      (jt.formatter-builder/parse-defaulting jt.chrono-field/hour-of-day 0)
      (jt.formatter-builder/parse-defaulting jt.chrono-field/offset-seconds 0)

      (jt.formatter-builder/to-formatter)))

(defn string->instant
  [x]
  (try
    (if (string? x)
      (-> (jt.formatter/parse instant-parse-formatter x)
          (jt.instant/from))
      x)
    (catch #?(:clj Throwable :cljs :default) _
      x)))

(defn instant->string
  [x]
  (cond->> x
    (instance? #?(:clj java.time.Instant :cljs js/java.time.Instant) x)
    (jt.formatter/format jt.formatter/iso-instant)))

(defn- string->local-date
  [x]
  (try
    (cond-> x
      (string? x)
      (jt.local-date/parse))
    (catch #?(:clj Throwable :cljs :default) _
      x)))

(defn- local-date->string
  [x]
  (cond->> x
    (instance? #?(:clj java.time.LocalDate :cljs js/java.time.LocalDate) x)
    (jt.formatter/format jt.formatter/iso-local-date)))

(defn string->maybe
  [x]
  (if (and (string? x) (= "null" x))
    nil
    x))

(def custom-string-transformer
  (mt/transformer
   {:name :hop-string
    :encoders {:vector {:leave vector->string}
               :tuple {:leave tuple->string}
               :string {:leave string->normalized-string}
               :hop/instant {:leave instant->string}
               :hop/local-date {:leave local-date->string}}
    :decoders {:maybe {:enter string->maybe}
               :vector {:enter string->vector}
               :tuple {:enter string->tuple}
               :string {:enter string->normalized-string}
               :hop/instant {:leave string->instant}
               :hop/local-date {:leave string->local-date}}}))

(def custom-json-transformer
  {:name :hop-json
   :encoders {:string string->normalized-string
              :hop/instant {:leave instant->string}
              :hop/local-date {:leave local-date->string}}
   :decoders {:string string->normalized-string
              :hop/instant {:leave string->instant}
              :hop/local-date {:leave string->local-date}}})

(def custom-reitit-string-transformer-provider
  (reify rcm/TransformationProvider
    (-transformer [_ {:keys [strip-extra-keys default-values]}]
      (mt/transformer
       (when strip-extra-keys (mt/strip-extra-keys-transformer))
       (mt/string-transformer)
       custom-string-transformer
       (when default-values (mt/default-value-transformer))))))

(def custom-reitit-json-transformer-provider
  (reify rcm/TransformationProvider
    (-transformer [_ {:keys [strip-extra-keys default-values]}]
      (mt/transformer
       (when strip-extra-keys (mt/strip-extra-keys-transformer))
       (mt/json-transformer)
       custom-json-transformer
       (when default-values (mt/default-value-transformer))))))

(def custom-reitit-malli-coercer
  (rcm/create
   {:error-keys #{:humanized}
    :compile mu/closed-schema
    :strip-extra-keys true
    :default-values true
    :lite false
    :encode-error (fn [error]
                    {:success? false
                     :reason :bad-parameter-type-or-format
                     :error-details (:humanized error)})
    :transformers {:body {:default rcm/default-transformer-provider
                          :formats {"application/json" custom-reitit-json-transformer-provider
                                    "application/transit+json" custom-reitit-json-transformer-provider}}
                   :response {:default rcm/default-transformer-provider}
                   :string {:default custom-reitit-string-transformer-provider}}}))
