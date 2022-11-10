;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.tooltip.loading-popup
  (:require [<<project.name>>.client.tooltip :as tooltip]
            [re-frame.core :as rf]))

(def ^:const popup-id "loading-popup")

(rf/reg-sub
 ::loading
 (fn [db _]
   (:loading db)))

(rf/reg-event-fx
 ::set-loading
 (fn [{:keys [db]} [_ message]]
   {:db (assoc db :loading {:message message})
    :dispatch [::tooltip/register {:id popup-id}]}))

(rf/reg-event-fx
 ::stop-loading
 (fn [{:keys [db]} _]
   {:db (dissoc db :loading)
    :dispatch [::tooltip/destroy-by-id {:id popup-id}]}))

(defn main []
  (let [loading (rf/subscribe [::loading])
        popup-data (rf/subscribe [::tooltip/by-id popup-id])]
    (fn []
      (when @popup-data
        [:div.loading-popup__backdrop
         {:class "loading-popup__backdrop--lightbox"
          :on-click #(.stopPropagation %)}
         [:div.loading-popup
          {:class (tooltip/gen-controller-class popup-id)}
          [:img.loading-popup__spinner
           {:src "/images/spinner.gif"}]
          (when-let [msg (:message @loading)]
            [:span msg])]]))))
