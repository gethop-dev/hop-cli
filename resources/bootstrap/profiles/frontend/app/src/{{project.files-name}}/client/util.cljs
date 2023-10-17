{{=<< >>=}}
(ns <<project.name>>.client.util
  (:require [<<project.name>>.client.tooltip.loading-popup :as loading-popup]
            [<<project.name>>.shared.util.transit :as util.transit]
            [ajax.core :as ajax]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 ::generic-success
 (fn [{:keys [db]} _]
   {:dispatch [::loading-popup/stop-loading]
    :db db}))

(rf/reg-event-fx
 ::generic-error
 (fn [{:keys [db]} [_ e]]
   {:dispatch [::loading-popup/stop-loading]
    :db (update db :errors-log (fnil conj []) e)}))

(defn ajax-transit-response-format
  []
  (ajax/transit-response-format
   {:handlers
    util.transit/custom-read-handlers}))

(defn ajax-transit-request-format
  []
  (ajax/transit-request-format
   {:handlers
    util.transit/custom-write-handlers}))
