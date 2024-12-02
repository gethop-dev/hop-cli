;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util
  (:refer-clojure :exclude [uuid])
  (:require [clojure.string :as str]
            [<<project.name>>.shared.util.string :as util.string]))

(defn uuid
  "If no argument is passed, creates a random UUID. If the passed
  paramenter is a UUID, returns it verbatim. If it is a string
  representing a UUID value return the corresponding UUID. Any other
  value or invalid string returns nil. "
  ([]
   (random-uuid))
  ([x]
   (try
     (cond
       (uuid? x)
       x

       (string? x)
       (parse-uuid x))
     (catch #?(:clj Throwable :cljs :default) _
       nil))))

(defn filter-map
  [m pred]
  (select-keys m (for [[k v] m :when (pred [k v])] k)))

(defn update-if-exists
  [m k update-fn & args]
  (if-not (= ::not-found (get m k ::not-found))
    (apply update m k update-fn args)
    m))

(defn update-if-not-nil
  [m k update-fn & args]
  (if-not (nil? (get m k))
    (apply update m k update-fn args)
    m))

(defn paginate-coll
  [{:keys [page page-size] :as _pagination} coll]
  (subvec
   (vec coll)
   (* page page-size)
   (min
    (count coll)
    (+ (* page page-size) page-size))))

(defn normalize-str-for-search
  [s]
  (-> (str s)
      (util.string/->nfc-normalized-unicode)
      (str/trim)
      (str/lower-case)))

(defn- search-match?
  [search-string search-keys m]
  (if (empty? search-string)
    true
    (some
     (fn [[_ v]]
       (str/includes? (normalize-str-for-search v) search-string))
     (select-keys m search-keys))))

(defn filter-search-coll
  [search-string search-keys coll]
  (let [search-string (normalize-str-for-search search-string)]
    (filter (partial search-match? search-string search-keys) coll)))

(defn remove-at-index
  "Remove element in coll by index."
  [coll idx]
  (vec (concat (subvec coll 0 idx)
               (subvec coll (inc idx)))))

(defn add-at-index
  "Add element in coll by index."
  [coll idx element]
  (concat (subvec coll 0 idx)
          [element]
          (subvec coll idx)))

(defn move-at-index
  "Move element in coll by index"
  [coll from-idx to-idx]
  (let [element-idx (nth coll from-idx)]
    (if (= from-idx to-idx)
      coll
      (into [] (add-at-index (remove-at-index coll from-idx) to-idx element-idx)))))
