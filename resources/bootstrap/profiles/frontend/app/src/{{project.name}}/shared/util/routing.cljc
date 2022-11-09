;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.routing
  (:require [malli.transform :as mt]
            [reitit.coercion.malli :as rcm]))

(defn singleton->vector
  [x]
  (if (string? x)
    (if (vector? x) x [x])
    x))

(def custom-string-type-decoders
  (assoc (mt/-string-decoders) :vector singleton->vector))

(def custom-string-transformer
  (mt/transformer
   {:name            :string
    :decoders        custom-string-type-decoders
    :encoders        mt/-string-decoders}))

(def custom-malli-coercion
  (rcm/create (assoc-in rcm/default-options
                        [:transformers :string :default]
                        custom-string-transformer)))
