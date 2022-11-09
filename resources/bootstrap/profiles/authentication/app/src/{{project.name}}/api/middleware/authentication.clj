;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.middleware.authentication
  (:require [buddy.auth :refer [authenticated?]]
            [<<project.name>>.api.util.responses :as util.r]))

(defn authentication-required
  "This would be the middleware after `auth-middleware` to check if the
  token verification was successfull."
  [handler]
  (fn [req]
    (if (authenticated? req)
      (handler req)
      (util.r/unauthorized {:error "Authentication required"}))))
