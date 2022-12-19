;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.keycloak.cli
  (:require [babashka.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [hop-cli.keycloak.api.openid-connect :as api.openid-connect]
            [hop-cli.keycloak.api.user :as api.user]
            [hop-cli.util :as util]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(defn with-access-token
  [opts call-fn]
  (let [result (api.openid-connect/get-admin-access-token opts)]
    (if-let [access-token (get result :access-token)]
      (call-fn (assoc opts :access-token access-token))
      {:success? false
       :reason :could-not-obtain-access-token
       :error-details result})))

(declare print-help-handler)

(def ^:const admin-auth-spec
  [[:base-url
    {:alias :bu :require true}]
   [:admin-realm-name
    {:alias :ar :default "master"}]
   [:admin-client-id
    {:alias :ac :default "admin-cli"}]
   [:admin-username
    {:alias :au :require true}]
   [:admin-password
    {:alias :ap :require true}]])

(defn- cli-cmd-table
  []
  [{:cmds ["create-user"]
    :fn #(-> (:opts %1)
             (update :attributes util/cli-stdin-map->map)
             (with-access-token api.user/create-user)
             (pprint))
    :error-fn error/generic-error-handler
    :desc "Creates a new user in the specified realm"
    :spec (concat
           admin-auth-spec
           [[:realm-name
             {:alias :r :require true}]
            [:username
             {:alias :u :require true}]
            [:temporary-password
             {:alias :p}]
            [:attributes
             {:alias :a
              :coerce []
              :desc "Attributes in the form of param1=value1 param2=value2..."}]
            [:first-name {}]
            [:last-name {}]
            [:email {}]
            [:email-verified {}]])}
   {:cmds ["set-user-password"]
    :fn #(-> (:opts %1)
             (with-access-token api.user/set-user-password)
             (pprint))
    :error-fn error/generic-error-handler
    :desc "Changes the password of the given user"
    :spec (concat
           admin-auth-spec
           [[:realm-name
             {:alias :r :require true}]
            [:user-id
             {:alias :u :require true}]
            [:password
             {:alias :p :require true}]
            [:temporary?
             {:alias :t}]])}
   {:cmds ["get-user"]
    :fn #(-> (:opts %1)
             (with-access-token api.user/get-user)
             (pprint))
    :error-fn error/generic-error-handler
    :desc "Returns the details about the given user"
    :spec (concat
           admin-auth-spec
           [[:realm-name
             {:alias :r :require true}]
            [:username
             {:alias :u :require true}]])}
   {:cmds ["get-id-token"]
    :fn #(pprint (api.openid-connect/get-id-token (:opts %1)))
    :error-fn error/generic-error-handler
    :desc "Returns id, access and refresh tokens for the given user credentials"
    :spec [[:base-url
            {:alias :bu :require true}]
           [:realm-name
            {:alias :r :require true}]
           [:client-id
            {:alias :c :require true}]
           [:username
            {:alias :u :require true}]
           [:password
            {:alias :p :require true}]]}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) "keycloak"))

(defn main [args]
  (cli/dispatch (cli-cmd-table) args))
