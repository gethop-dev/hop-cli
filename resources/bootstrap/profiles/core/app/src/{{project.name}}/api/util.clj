;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/
{{=<< >>=}}
(ns <<project.name>>.api.util
  (:require [buddy.auth :as auth]))

(defn- restrict-fn
  "Restrict access to the handler. Only allow access if the request
  contains a valid identity that has already been checked."
  [handler]
  (fn [req]
    (if (auth/authenticated? req)
      (handler req)
      {:status 401
       :body {:error "Authentication required"}
       :headers {"content-type" "application/json"}})))

(defn authentication-required
  "This would be the middleware after `auth-middleware` to check if the
  token verification was successfull."
  []
  (fn [handler]
    (fn [req]
      (prn "Check if user is authorized.")
      (handler req))))
