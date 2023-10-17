;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.user
  (:require [<<project.name>>.client.util :as util]
            [dev.gethop.session.re-frame.cognito.token :as session.token]
            [re-frame.core :as rf]))

(rf/reg-sub
 ::user-data
 (fn [db _]
   (get db :user)))

(rf/reg-event-db
 ::set-user-data
 (fn [db [_ user]]
   (assoc db :user user)))

(defn- fetch-user-data-event-fx
  "This event handler gets jwt-token from session cofx instead of appdb.
  It is so because at times the token may not be present in appdb yet when
  ::ensure-data is called."
  [{:keys [session] :as _cofx} _]
  {:http-xhrio {:headers {"Authorization" (str "Bearer " (:id-token session))}
                :method :get
                :uri "/api/user"
                :response-format (util/ajax-transit-response-format)
                :on-success [::set-user-data]
                :on-failure [::util/generic-error]}})

(rf/reg-event-fx
 ::fetch-user-data
 [(rf/inject-cofx ::session.token/session)]
 fetch-user-data-event-fx)
