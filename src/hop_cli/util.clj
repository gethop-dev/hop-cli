(ns hop-cli.util
  (:require [clojure.string :as str]
            [clojure.walk :as walk]))

(defn cli-stdin-map->map
  [stdin-parameters]
  (reduce (fn [acc s]
            (let [[k v] (str/split s #"=" 2)]
              (assoc acc (keyword k) v)))
          {}
          stdin-parameters))

(defn update-map-vals
  ([m update-fn & {:keys [recursive?] :or {recursive? true}}]
   (if recursive?
     (walk/postwalk
      (fn [x] (if (and (map? x) (not (record? x)))
                (update-map-vals x update-fn :recursive? false) x)) m)
     (reduce-kv #(assoc %1 %2 (update-fn %3)) {} m))))
