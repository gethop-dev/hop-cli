;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.session.user
  (:require [<<project.name>>.client.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub
 ::user-data
 (fn [db _]
   (get-in db [:session :user])))

(rf/reg-event-db
 ::set-user-data
 (fn [db [_ data]]
   (assoc-in db [:session :user] data)))

(rf/reg-event-fx
 ::on-user-data-load-success
 (fn [{:keys [db]} [_ on-success-evt {:keys [user]}]]
   (cond-> {:db (assoc-in db [:session :user] user)}
     on-success-evt
     (assoc :fx [[:dispatch on-success-evt]]))))

(rf/reg-event-fx
 ::fetch-user-data
 [(rf/inject-cofx :session)]
 (fn [{:keys [session]} [_ & {:keys [on-success-evt]}]]
   {:http-xhrio {:headers {"Authorization" (str "Bearer " (:id-token session))}
                 :method :get
                 :uri "/api/user"
                 :response-format (util/ajax-transit-response-format)
                 :on-success [::on-user-data-load-success on-success-evt]
                 :on-failure [::util/generic-error]}}))
