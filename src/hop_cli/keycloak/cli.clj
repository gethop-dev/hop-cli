;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.keycloak.cli
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [clojure.pprint :refer [pprint]]
            [hop-cli.keycloak.api.openid-connect :as api.openid-connect]
            [hop-cli.keycloak.api.user :as api.user]
            [hop-cli.util :as util]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(def ^:const main-cmd
  "keycloak")

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
  [[:admin-realm-name
    {:alias :ar
     :default "master"
     :default-desc "(Optional, default value: master)"
     :desc "Name of the Keycloak Realm where the 'admin-user' exists."}]
   [:admin-client-id
    {:alias :ac
     :default "admin-cli"
     :default-desc "(Optional, default value: admin-cli)"
     :desc "Client Id to use with the 'admin-realm-name'."}]
   [:admin-client-secret
    {:alias :acs
     :default-desc "(Optional, default empty)"
     :desc "Client Secret to use with the 'admin-realm-name'."}]
   [:admin-username
    {:alias :au
     :require true
     :desc "Username with administrator rights to perform the action."}]
   [:admin-password
    {:alias :ap
     :require true
     :desc "Password for the `admin-username`."}]])

(def ^:const common-params-spec
  [[:base-url
    {:alias :bu
     :require true
     :desc "Base URL for the Keycloak instance."}]
   [:realm-name
    {:alias :r
     :require true
     :desc "Keycloak Realm to use for the action."}]
   [:insecure-connection
    {:alias :k
     :coerce :boolean
     :require false
     :desc "Skip SSL certificate verification step."
     :default-desc "(Optional)"}]
   [:cacert
    {:ref "<path>"
     :require false
     :validate {:pred (fn [path]
                        (and (fs/exists? path) (fs/readable? path)))
                :ex-msg (fn [m]
                          (format "'%s' does not point to a file that can be read."
                                  (:value m)))}
     :desc "The file at <path> contains the CA certificates (in PEM format) that will be used to validate the Keycloak server certificate."
     :default-desc "(Optional)"}]])

(def ^:const realm-name-spec
  [:realm-name
   {:alias :r
    :require true
    :desc "Keycloak Realm to use for the action."}])

(defn- cli-cmd-table
  []
  [{:cmds ["create-user"]
    :fn #(-> (:opts %1)
             (update :attributes util/cli-stdin-map->map)
             (with-access-token api.user/create-user)
             (pprint))
    :error-fn (partial error/generic-error-handler [main-cmd "create-user"])
    :desc "Create a user in the specified Keycloak Realm."
    :spec (help/with-help-spec
            (concat
             common-params-spec
             admin-auth-spec
             [[:username
               {:alias :u
                :require true
                :desc "Username for the new user to be created."}]
              [:temporary-password
               {:alias :p
                :require true
                :desc "Temporary password assigned to the user. It will have to be changed on first login."}]
              [:attributes
               {:alias :a
                :coerce []
                :desc "User attributes in the form of 'ATTRIBUTE1=VAL1 ATTRIBUTE2=VAL2'"
                :default-desc "(Optional, default empty)"}]
              [:first-name
               {:desc "First name for the new user to be created."
                :default-desc "(Optional, default empty)"}]
              [:last-name
               {:desc "Last name for the new user to be created."
                :default-desc "(Optional, default empty)"}]
              [:email
               {:desc "Email address for the new user to be created."
                :default-desc "(Optional, default empty)"}]
              [:email-verified
               {:desc "Set the new use email address as verified."
                :coerce :boolean
                :default false
                :default-desc "(Optional, default false)"}]]))}
   {:cmds ["set-user-password"]
    :fn #(-> (:opts %1)
             (with-access-token api.user/set-user-password)
             (pprint))
    :error-fn (partial error/generic-error-handler [main-cmd "set-user-password"])
    :desc "Change the password of the specified user."
    :spec (help/with-help-spec
            (concat
             common-params-spec
             admin-auth-spec
             [[:user-id
               {:alias :u
                :require true
                :desc "Keycloak internal ID for the user (you can get it with the 'get-user' command)."}]
              [:password
               {:alias :p
                :require true
                :desc "New password to be assigned to the user"}]
              [:temporary?
               {:alias :t
                :coerce :boolean
                :desc "Whether the new password is temporary. If so, it will have to be changed on first login."}]]))}
   {:cmds ["get-user"]
    :fn #(-> (:opts %1)
             (with-access-token api.user/get-user)
             (pprint))
    :error-fn (partial error/generic-error-handler [main-cmd "get-user"])
    :desc "Get the details about the specified Keycloak user."
    :spec (help/with-help-spec
            (concat
             common-params-spec
             admin-auth-spec
             [[:username
               {:alias :u
                :require true
                :desc "What username to perform the operation for."}]]))}
   {:cmds ["get-id-token"]
    :fn (fn [{:keys [opts]}]
          (let [result (api.openid-connect/get-id-token opts)]
            (if (and (:raw opts) (:success? result))
              (println (:id-token result))
              (pprint result))))
    :error-fn (partial error/generic-error-handler [main-cmd "get-id-token"])
    :desc "Get an OpenID Connect Identity token for the specified user."
    :spec (help/with-help-spec
            (concat
             common-params-spec
             [[:client-id
               {:alias :c
                :require true
                :desc "Client ID to use to get the Identity token."}]
              [:client-secret
               {:alias :cs
                :require false
                :desc "Client Secret to use to get the Identity token."
                :default-desc "(Optional, default empty)"}]
              [:username
               {:alias :u
                :require true
                :desc "What username to get the token for."}]
              [:password
               {:alias :p
                :require true
                :desc "The username's password."}]
              [:raw
               {:require false
                :default false
                :coerce :boolean
                :desc "Output id-token in raw format."
                :default-desc "(Optional)"}]]))}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) main-cmd))

(defn main [args]
  (cli/dispatch (cli-cmd-table) args))
