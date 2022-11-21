(ns hop-cli.bootstrap.settings-reader
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [hop-cli.bootstrap.util :as bp.util]
            [hop-cli.util :as util]
            [hop-cli.util.random :as util.random]
            [malli.core :as m])
  (:import (java.io PushbackReader)))

(def setting-name-schema
  simple-keyword?)

(def setting-docstring-schema
  [:vector {:min 1} :any])

(def setting-min-hop-version-schema
  [:string {:min 1}])

(def setting-tag-schema
  [:string {:min 1}])

(def setting-type-schema
  [:enum
   ;; non-group values
   :integer :nat-int :float :number :string
   :regexp :char :boolean :symbol :list
   :vector :map :set :uuid :inst
   :password :auto-gen-password
   ;; group values
   :plain-group :single-choice-group :multiple-choice-group])

(def setting-common-schema
  [:map
   ;; Optional property telling Malli that the map schema is
   ;; closed. Only the keys defined here can appear in the data to be
   ;; validated. In this case, it means there can't be additional keys
   ;; in the map, only the ones defined here. If there are, malli
   ;; raises an error. This is the opposite of what s/keys does in
   ;; spec. s/keys allows for open map specs, meaning it only
   ;; validates the keys you specify, but doesn't care about other
   ;; possibly existing keys, and doesn't complain about them.
   #_{:closed true}

   ;; Now the actual specs for the keys and their values.
   [:name setting-name-schema]
   [:docstring {:optional true}  setting-docstring-schema]
   [:min-hop-version {:optional true} setting-min-hop-version-schema]
   [:tag {:optional true} setting-tag-schema]
   [:type setting-type-schema]])

(def setting-value-integer-schema
  ;; Built-in, in malli.core/predicate-schemas
  integer?)

(def setting-value-nat-int-schema
  ;; Built-in, in malli.core/predicate-schemas
  nat-int?)

(def setting-value-float-schema
  ;; Built-in, in malli.core/predicate-schemas
  float?)

(def setting-value-number-schema
  ;; Built-in, in malli.core/predicate-schemas
  number?)

(def setting-value-string-schema
  ;; Built-in, in malli.core/predicate-schemas
  string?)

(def setting-value-regexp-schema
  [:and
   ;; Built-in, in malli.core/predicate-schemas
   string?
   ;; Custom function schema.
   [:fn re-pattern]])

(def setting-value-char-schema
  ;; Built-in, in malli.core/predicate-schemas
  char?)

(def setting-value-boolean-schema
  ;; Built-in, in malli.core/predicate-schemas
  boolean?)

(def setting-value-symbol-schema
  ;; Built-in, in malli.core/predicate-schemas
  symbol?)

(def setting-value-list-schema
  ;; Built-in, in malli.core/predicate-schemas
  list?)

(def setting-value-vector-schema
  ;; Built-in, in malli.core/predicate-schemas
  vector?)

(def setting-value-map-schema
  ;; Built-in, in malli.core/predicate-schemas
  map?)

(def setting-value-set-schema
  ;; Built-in, in malli.core/predicate-schemas
  set?)

(def setting-value-uuid-schema
  ;; Built-in, in malli.core/predicate-schemas
  uuid?)

(def setting-value-inst-schema
  ;; Built-in, in malli.core/predicate-schemas
  inst?)

(def setting-value-password-schema
  string?)

(def setting-value-auto-gen-password-schema
  [:map
   [:length pos-int?]])

(def setting-schema
  (m/schema
   ;; Introduce a local registry, so we can have recursive schemas for
   ;; the `:plain-group` type. See https://github.com/metosin/malli#recursive-schemas
   ;; for details.
   [:schema {:registry
             {::setting-schema
              [:multi {:dispatch :type}
               [:integer (conj setting-common-schema [:value setting-value-integer-schema])]
               [:nat-int (conj setting-common-schema [:value setting-value-nat-int-schema])]
               [:float (conj setting-common-schema [:value setting-value-float-schema])]
               [:number (conj setting-common-schema [:value setting-value-number-schema])]
               [:string (conj setting-common-schema [:value setting-value-string-schema])]
               [:regexp (conj setting-common-schema [:value setting-value-regexp-schema])]
               [:char (conj setting-common-schema [:value setting-value-char-schema])]
               [:boolean (conj setting-common-schema [:value setting-value-boolean-schema])]
               [:symbol (conj setting-common-schema [:value setting-value-symbol-schema])]
               [:list (conj setting-common-schema [:value setting-value-list-schema])]
               [:vector (conj setting-common-schema [:value setting-value-vector-schema])]
               [:map (conj setting-common-schema [:value setting-value-map-schema])]
               [:set (conj setting-common-schema [:value setting-value-set-schema])]
               [:uuid (conj setting-common-schema [:value setting-value-uuid-schema])]
               [:inst (conj setting-common-schema [:value setting-value-inst-schema])]
               [:password (conj setting-common-schema [:value setting-value-password-schema])]
               [:auto-gen-password (conj setting-common-schema [:value setting-value-auto-gen-password-schema])]
               [:plain-group
                ;; `:plain-group` key is special, as it contains a vector of other
                ;; `settings-schema`. In this case we need to use a local registry
                ;; (defined above) to define a qualified keyword to name the schema,
                ;; and `:ref` to recursively refer to it.
                (conj setting-common-schema [:value [:vector [:ref ::setting-schema]]])]
               [:single-choice-group
                ;; `:single-choice-group` key is also special, as it contains a
                ;; vector of other `settings-schema` in the `:choices` key.
                (conj setting-common-schema
                      [:choices [:vector {:min 1} [:ref ::setting-schema]]]
                      [:value setting-name-schema])]
               [:multiple-choice-group
                ;; `:multiple-choice-group` key is also special, as it contains a
                ;; vector of other `settings-schema` in the `:choices` key.
                (conj setting-common-schema
                      [:choices [:vector {:min 1} [:ref ::setting-schema]]]
                      [:value [:vector setting-name-schema]])]]}}
    ::setting-schema]))

(def settings-schema
  (m/schema [:vector {:min 1} setting-schema]))

(defprotocol RefLike
  (ref-key [r] "Return the key of the reference.")
  (ref-resolve [r config] "Return the resolved value."))

(defrecord Ref [key]
  RefLike
  (ref-key [_] key)
  (ref-resolve [_ settings]
    (bp.util/get-settings-value settings key)))

(defn resolve-refs
  [settings subpath]
  (update-in
   settings
   subpath
   util/update-map-vals
   (fn [v]
     (if-not (instance? RefLike v)
       v
       (ref-resolve v settings)))))

(defn- settings->settings-nested-map
  [settings]
  (->> settings
       (walk/postwalk
        (fn [node]
          (if-not (and (map? node) (:name node))
            node
            {(:name node)
             (cond
               (= (:type node) :plain-group)
               (apply merge (:value node))

               (= (:type node) :single-choice-group)
               (->
                (first (filter #(= (:value node) (first (keys %))) (:choices node)))
                (assoc :value (:value node)))

               (= (:type node) :multiple-choice-group)
               (->
                (apply merge (filter #(get (set (:value node)) (first (keys %))) (:choices node)))
                (assoc :value (:value node)))

               :else
               (:value node))})))
       (apply merge)))

(defn- inject-auto-generated-passwords
  [{:keys [type value] :as node}]
  (if (= :auto-gen-password type)
    (assoc node :value (util.random/generate-random-password value))
    node))

(defn- build-refs
  [{:keys [type value] :as node}]
  (if (= :ref type)
    (assoc node :value (->Ref value))
    node))

(defn read-settings
  [settings-file-path]
  (let [settings (->> settings-file-path
                      (fs/file)
                      (io/reader)
                      (PushbackReader.)
                      (edn/read))]
    (if true #_(m/validate settings-schema settings)
        {:success? true
         :settings (->> settings
                        (walk/prewalk (comp build-refs inject-auto-generated-passwords))
                        (settings->settings-nested-map))}
        {:success? false
         :error-details (m/explain settings-schema settings)})))
