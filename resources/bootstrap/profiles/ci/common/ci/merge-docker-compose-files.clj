#!/usr/bin/env bb

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; deep-coll-merge-with, distinct-concat-coll-merge,                 ;;
;; distinct-merge-with and distinct-merge are Copyright 2019 Jason   ;;
;; Stiefel, licensed under the Eclipse Public License 1.0.           ;;
;;                                                                   ;;
;; The rest of the code is under the same license as the rest of HOP ;;
;; CLI.                                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require '[clj-yaml.core :as yaml]
         '[clojure.java.io :as io])

(defn- deep-coll-merge-with
  "The base method used for recursive collection merging"
  [collection-merge-method non-collection-merge-method & vals]
  (let [this-method (partial deep-coll-merge-with collection-merge-method non-collection-merge-method)]
    (cond
      (every? map? vals) (apply merge-with this-method vals)
      (every? coll? vals) (apply collection-merge-method vals)
      :else (apply non-collection-merge-method vals))))

(defn- distinct-concat-coll-merge
  "Puts all items from all args into one deduplicated vector"
  [& args]
  (->> args
       (apply concat)
       (distinct)
       (into [])))

(def distinct-merge-with (partial deep-coll-merge-with distinct-concat-coll-merge))
(def distinct-merge (partial distinct-merge-with #(last %&)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Our own code starts here ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- usage
  []
  (println (format "Usage: %s docker-compose.file-1.yml [docker-compose.file-2.yml ...]\n"
                   (-> *file* (io/file) (.toPath) (.getFileName) (.toString)))))

(defn- parse-yaml-file
  [file]
  (-> file (io/file) (io/reader) (yaml/parse-stream)))

(let [files *command-line-args*]
  (if-not (seq files)
    (usage)
    (let [merged-file (reduce (fn [acc file]
                                (distinct-merge acc (parse-yaml-file file)))
                              (parse-yaml-file (first files))
                              (rest files))]
      (-> merged-file
          (yaml/generate-string :dumper-options {:flow-style :block})
          println))))
