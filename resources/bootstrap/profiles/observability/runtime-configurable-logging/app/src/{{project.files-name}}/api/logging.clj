;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.logging
  (:require [<<project.name>>.service.timbre :as srv.timbre]
            [<<project.name>>.api.util.responses :as util.r]
            [duct.logger :refer [log]]
            [integrant.core :as ig]))

(def logging-level-keyword
  [:and
   [:keyword]
   [:enum
    :trace :debug :info :warn :error :fatal :report]])

(def logging-level-ns-pattern
  [:or
   string?
   [:set string?]])

(def logging-level-coll
  [:vector
   {:min 1}
   [:tuple
    logging-level-ns-pattern
    logging-level-keyword]])

(def logging-config-schema
  [:map
   [:level
    {:optional true}
    [:or
     logging-level-keyword
     logging-level-coll]]])

(defn- get-config
  [{:keys [logger]} _req]
  (log logger :info ::getting-current-logging-config)
  (let [result (srv.timbre/get-config)]
    (if (:success? result)
      (util.r/ok result)
      (util.r/server-error result))))

(defn- set-config
  [{:keys [logger]} req]
  (let [new-config (get-in req [:parameters :body :config])
        prv-config (:config (srv.timbre/get-config))
        result (srv.timbre/set-config new-config)]
    (log logger :info ::previous-logging-config prv-config)
    (log logger :info ::new-logging-config new-config)
    (if (:success? result)
      (util.r/ok
       {:success? true
        :previous-config prv-config
        :config new-config})
      (util.r/server-error result))))

;; TODO The API endpoints must only be available for privileged users.
;; This fake middleware will always deny the access to the API.
;; So you must implement your own authentication/authorization middleware.
(defn- replace-me-with-auth-middleware
  [_handler]
  (fn [_req]
    (util.r/unauthorized {})))

(defmethod ig/init-key :<<project.name>>.api/logging
  [_ config]
  ["/logging/config"
   {:get {:summary "Get the logging configuration"
          :swagger {:tags ["logging"]}
          :middleware [replace-me-with-auth-middleware]
          :handler (partial get-config config)
          :responses {200 {:body [:map
                                  [:success? boolean?]
                                  [:config logging-config-schema]]}}}
    :post {:summary "Update the logging configuration"
           :swagger {:tags ["logging"]}
           :middleware [replace-me-with-auth-middleware]
           :handler (partial set-config config)
           :parameters {:body [:map [:config logging-config-schema]]}
           :responses {200 {:body [:map
                                   [:success? boolean?]
                                   [:config logging-config-schema]
                                   [:previous-config logging-config-schema]]}}}}])
