;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.tooltip
  (:require [<<project.name>>.shared.util :as shared.util]
            [clojure.string :as str]
            [re-frame.core :as rf]))

(def ^:const controller-class-prefix "js-tooltip-controller-")
(def ^:const controller-class-pattern
  (re-pattern (str controller-class-prefix "([\\w\\-]+)")))

(defn close-popup-component
  [config]
  [:img.icon
   {:class "close-popup-icon"
    :src "/images/close-white.svg"
    :on-click #(rf/dispatch [::destroy-by-id config])}])

(rf/reg-sub
 ::controls
 (fn [db _]
   (:tooltip-controls db)))

(rf/reg-sub
 ::by-id
 (fn [_ _]
   (rf/subscribe [::controls]))
 (fn [tooltips-controls [_ id]]
   (get tooltips-controls id)))

(defn default-tooltip-data []
  {:id (str (random-uuid))
   :destroy-on-click-out? true})

(rf/reg-event-db
 ::register
 (fn [db [_ data]]
   (let [{:keys [id group] :as data} (merge (default-tooltip-data) data)]
     (update db :tooltip-controls
             (fn [tooltip-controls]
               (cond-> tooltip-controls
                 group
                 (shared.util/filter-map (fn [[_ v]] (not= group (:group v))))
                 :always
                 (assoc id data)))))))

(rf/reg-event-fx
 ::destroy-by-id
 (fn [{:keys [db]} [_ {:keys [id on-destroy-evt]}]]
   (merge {:db (update db :tooltip-controls dissoc id)}
          (when on-destroy-evt
            {:dispatch on-destroy-evt}))))

(rf/reg-event-db
 ::destroy-by-ids
 (fn [db [_ ids]]
   (update db :tooltip-controls
           #(apply dissoc % ids))))

(rf/reg-event-fx
 ::dispatch-on-destroy-events
 (fn [_ [_ on-destroy-events]]
   {:dispatch-n on-destroy-events}))

(defn find-tooltip-controller-class-in-node [node]
  (let [class-name (.-className node)]
    (when (string? class-name)
      (first (re-find controller-class-pattern class-name)))))

(defn find-tooltip-controller-class [node]
  (or (find-tooltip-controller-class-in-node node)
      (some-> (.-parentNode node) (find-tooltip-controller-class))))

(defn- find-tooltips-to-destroy
  [controls clicked-controller]
  (reduce (fn [acc {:keys [id on-destroy-evt]}]
            (if-not (= clicked-controller id)
              (-> acc
                  (update :ids-to-destroy #(conj % id))
                  (update :on-destroy-events #(conj % on-destroy-evt)))
              acc))
          {:ids-to-destroy []
           :on-destroy-events []}
          controls))

(defn destroy-on-click-out [clicked-node]
  (let [clicked-controller (some->
                            (find-tooltip-controller-class clicked-node)
                            (str/split controller-class-prefix)
                            (second))
        controls (->> @(rf/subscribe [::controls])
                      (vals)
                      (filter :destroy-on-click-out?))
        {:keys [ids-to-destroy on-destroy-events]} (find-tooltips-to-destroy controls clicked-controller)]
    (when (seq ids-to-destroy)
      (rf/dispatch [::destroy-by-ids ids-to-destroy]))
    (when (seq on-destroy-events)
      (rf/dispatch [::dispatch-on-destroy-events on-destroy-events]))))

(defn gen-controller-class [tooltip-id]
  {:pre [(string? tooltip-id)]}
  (str controller-class-prefix tooltip-id))
