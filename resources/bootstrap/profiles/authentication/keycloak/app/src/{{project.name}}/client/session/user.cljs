;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.session.user
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [<<project.name>>.client.util :as util]))

(rf/reg-sub
 ::user-data
 (fn [db _]
   (get-in db [:session :user])))

(rf/reg-event-db
 ::set-user-data
 (fn [db [_ data]]
   (assoc-in db [:session :user] data)))

(rf/reg-event-fx
 ::on-user-data-loaded
 (fn [{:keys [db]} [_ redirect-url {:keys [user]}]]
   (cond-> {:db (assoc-in db [:session :user] user)}
     redirect-url
     (assoc :redirect redirect-url))))

(rf/reg-event-fx
 ::fetch-user-data
 (fn [{:keys [db]} [_ redirect-url]]
   {:http-xhrio {:headers {"Authorization" (str "Bearer " (:jwt-token db))}
                 :method :get
                 :uri "/api/user"
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::on-user-data-loaded redirect-url]
                 :on-failure [::util/generic-error]}}))
