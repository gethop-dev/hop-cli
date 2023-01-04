(ns settings
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))

(rf/reg-sub
 ::settings
 (fn [db _]
   (get db :settings)))

(rf/reg-event-db
 ::set-settings
 (fn [db [_ settings]]
   (assoc db :settings settings)))

(rf/reg-event-db
 ::update-settings-value
 (fn [db [_ path value]]
   (assoc-in db (cons :settings (conj path :value)) value)))

(defn build-node-id
  [path]
  (str/join "_" path))

(defn group?
  [node]
  (get #{:plain-group :single-choice-group :multiple-choice-group}
       (:type node)))

(defn get-selected-single-choice
  [node]
  (some (fn [[index choice]]
          (when (= (:name choice) (:value node))
            [index choice]))
        (keep-indexed vector (:choices node))))

(defn get-selected-multiple-choices
  [node]
  (filter (fn [[index choice]]
            (when (get (set (:value node)) (:name choice))
              [index choice]))
          (keep-indexed vector (:choices node))))

(defn get-node-path
  [settings node-name-path]
  (loop [settings settings
         node-name-path node-name-path
         path []]
    (if-not (seq node-name-path)
      path
      (let [next-node-name (first node-name-path)
            children-key (if (= (:type settings) :plain-group)
                           :value
                           :choices)
            children-nodes (if (vector? settings)
                             settings
                             (get settings children-key))
            [next-index next-node] (some (fn [[index node]]
                                           (when (= (:name node) next-node-name)
                                             [index node]))
                                         (keep-indexed vector children-nodes))]
        (when next-node
          (recur next-node
                 (rest node-name-path)
                 (if (vector? settings)
                   (conj path next-index)
                   (conj path children-key next-index))))))))

(defn toggle-value
  [old-values new-value]
  (if (get (set old-values) new-value)
    (remove #{new-value} old-values)
    (conj old-values new-value)))
