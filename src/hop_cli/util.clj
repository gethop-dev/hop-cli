(ns hop-cli.util
  (:require [clojure.string :as str]))

(defn merge-with-key
  [f & maps]
  (reduce
   (fn [m1 m2]
     (reduce-kv
      (fn [m k v]
        (if (contains? m k)
          (assoc m k (f k (get m k) v))
          (assoc m k v)))
      m1 m2))
   maps))

(defn expand-ns-keywords
  [m]
  (reduce-kv
   (fn [m k v]
     (if (simple-keyword? k)
       (assoc m k v)
       (let [ns-keys (str/split (namespace k) #"\.")
             all-keys (conj ns-keys (name k))
             keywords (map keyword all-keys)]
         (assoc-in m keywords v))))
   {}
   m))
