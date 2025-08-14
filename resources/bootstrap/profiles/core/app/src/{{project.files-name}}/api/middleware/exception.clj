;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.middleware.exception
  (:require [duct.logger :refer [log]]
            [reitit.ring.middleware.exception :as exception]))

(defn- exception-handler-wrapper
  [logger handler e request]
  (log logger :error {:uri (:uri request)
                      :method (:request-method request)
                      :exception e})
  (handler e request))

(defn create-exception-middleware
  [logger]
  (exception/create-exception-middleware
   (assoc exception/default-handlers
          ::exception/wrap (partial exception-handler-wrapper logger))))
