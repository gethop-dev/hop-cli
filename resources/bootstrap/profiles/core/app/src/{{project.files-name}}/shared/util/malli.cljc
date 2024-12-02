;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.malli
  (:refer-clojure :exclude [dissoc])
  (:require [malli.core :as m]
            [malli.util :as mu]))

(defn dissoc
  "Like [[malli.util/dissoc]] but accepts a sequence of keys `ks` to be
  dissociated from the schema."
  ([?schema ks]
   (dissoc ?schema ks nil))
  ([?schema ks options]
   (mu/transform-entries ?schema #(remove (fn [[k]] (get (set ks) k)) %) options)))

(defn filter-map-schema
  [schema pred?]
  (let [ks (->> (m/children schema)
                (filter pred?)
                (map first))]
    (mu/select-keys schema ks)))
