;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.malli-coercion
  (:require [<<project.name>>.shared.util.string :as util.string]
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
    (str/split #";")))

(defn- tuple->string
  [x]
  (cond->> x
    (vector? x)
    (str/join ";")))

(defn- string->normalized-string
  [x]
  (cond-> x
    (string? x)
    (util.string/->nfc-normalized-unicode)))

(def custom-string-transformer
  (mt/transformer
   {:name :biotz-string
    :encoders {:vector {:leave vector->string}
               :tuple {:leave tuple->string}
               :string {:leave string->normalized-string}}
    :decoders {:vector {:enter string->vector}
               :tuple {:enter string->tuple}
               :string {:enter string->normalized-string}}}))

(def custom-json-transformer
  {:name :biotz-json
   :encoders {:string string->normalized-string}
   :decoders {:string string->normalized-string}})

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
