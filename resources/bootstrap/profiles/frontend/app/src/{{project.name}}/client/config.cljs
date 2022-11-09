;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.config)

(rf/reg-event-fx
 ::config-loaded
 (fn [{:keys [db]} [_ on-success-evt config]]
   (merge
    {:db (assoc db :config config)}
    (when on-success-evt
      {:dispatch on-success-evt}))))

(rf/reg-event-fx
 ::load-config
 (fn [_ [_ on-success-evt]]
   {:http-xhrio {:method :get
                 :uri "/api/config"
                 :format (ajax/transit-request-format)
                 :response-format (ajax/transit-response-format)
                 :on-success [::config-loaded on-success-evt]
                 :on-failure [::util/generic-error]}}))
