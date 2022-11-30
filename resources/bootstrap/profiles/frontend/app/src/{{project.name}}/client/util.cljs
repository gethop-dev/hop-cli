{{=<< >>=}}
(ns <<project.name>>.client.util
  (:require [<<project.name>>.client.tooltip.loading-popup :as loading-popup]
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
