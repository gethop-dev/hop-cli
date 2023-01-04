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
