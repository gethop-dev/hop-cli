;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.domain.types
  (:require [malli.core :as m]))

(def ^:const enum-types
  "Set of custom enum types defined in the data application model,
  that need to be mapped to and from SQL enums.

  For example, we may have a \"user type\" attribute for the \"user\"
  domain entity. That attribute is named `:user-type` in the domain
  entity model, and the possible values are the set
  `#{:admin :plant-manager :asset-manager}`. In the SQL database we
  would define an enum type as:

    CREATE TYPE user_type AS ENUM ('admin', 'plant-manager', 'asset-manager');

  and in this map we would add the key `:user-type` with the value
  `#{:admin :plant-manager :asset-manager}`"
  {:some-enum-type-name #{:first-enum-value
                          :second-enum-value}})

(defn get-type-schema
  [type-name]
  (m/schema
   [:and
    [:keyword]
    (apply conj [:enum] (get enum-types type-name))]))
