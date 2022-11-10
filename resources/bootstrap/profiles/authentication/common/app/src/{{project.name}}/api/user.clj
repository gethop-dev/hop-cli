;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.user
  (:require [integrant.core :as ig]
            [<<project.name>>.api.middleware.authentication :as api.auth]
            [<<project.name>>.api.util.responses :as util.r]
            [<<project.name>>.util :as util]))

(defn- get-dummy-user-data
  [config req]
  (let [user-id (-> req :identity util/uuid)]
    (util.r/ok {:success? true
                :user {:avatar "https://www.w3schools.com/w3images/avatar2.png"
                       :first-name "John"
                       :last-name "Doe"
                       :id user-id}})))

(defmethod ig/init-key :<<project.name>>.api/user
  [_ {:keys [auth-middleware] :as config}]
  ["/user"
   {:get {:summary "Get user details"
          :middleware [auth-middleware
                       api.auth/authentication-required]
          :swagger {:tags ["user"]}
          :handler (partial get-dummy-user-data config)}}])
