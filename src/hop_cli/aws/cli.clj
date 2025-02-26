;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.cli
  (:require [babashka.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [hop-cli.aws.cognito :as cognito]
            [hop-cli.aws.env-vars :as env-vars]
            [hop-cli.aws.rds :as rds]
            [hop-cli.aws.ssl :as ssl]
            [hop-cli.util :as util]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(def ^:const main-cmd
  "aws")

(defn- generic-handler-wrapper
  [handler-fn {:keys [opts]}]
  (pprint (handler-fn opts)))

(defn- cognito-create-user-handler
  [{:keys [opts]}]
  (let [parsed-opts (update opts :attributes util/cli-stdin-map->map)]
    (pprint (cognito/admin-create-user parsed-opts))))

(declare print-help-handler)

(def ^:const region-spec
  [:region
   {:alias :r
    :desc "AWS Region where the resource is defined."
    :default-desc "Optional (default empty)"}])

(def ^:const project-name-spec
  [:project-name
   {:alias :p
    :require true
    :desc "Name of the project for the action."}])

(def ^:const environment-spec
  [:environment
   {:alias :e
    :desc "Target environment for the action (get, sync, etc.). E.g., prod, test."
    :require true}])

(def kms-key-alias-spec
  [:kms-key-alias
   {:alias :k :require true
    :desc "Alias of the AWS KMS Key that will be used to encrypt or decrypt the environment variables."}])
(def ^:const file-spec
  [:file
   {:alias :f
    :require true
    :desc "Path where the environment variables will be saved to or loaded from."}])

(def ^:const user-pool-id-spec
  [:user-pool-id
   {:alias :up
    :require true
    :desc "ID of the AWS Cognito User Pool where the action will be performed."}])

(def ^:const username-spec
  [:username
   {:alias :u
    :require true
    :desc "Username or email to use for the action."}])

(def ^:const password-spec
  [:password
   {:alias :p
    :require true
    :desc ""}])

(defn- cli-cmd-table
  []
  [;; Environment Variable manager
   {:cmds ["env-vars" "sync"]
    :fn (partial generic-handler-wrapper env-vars/sync-env-vars)
    :error-fn (partial error/generic-error-handler [main-cmd "env-vars" "sync"])
    :desc "Command for synchronizing local environment variables with the remote environment variables storage in AWS SSM Parameter Store."
    :spec (help/with-help-spec
            [project-name-spec
             environment-spec
             file-spec
             kms-key-alias-spec
             region-spec])}
   {:cmds ["env-vars" "download"]
    :fn (partial generic-handler-wrapper env-vars/download-env-vars)
    :error-fn (partial error/generic-error-handler [main-cmd "env-vars" "download"])
    :desc "Command for downloading the environment variables from AWS SSM Parameter Store into a file."
    :spec (help/with-help-spec
            [project-name-spec
             environment-spec
             file-spec
             kms-key-alias-spec
             region-spec])}
   {:cmds ["env-vars" "apply-changes"]
    :fn (partial generic-handler-wrapper env-vars/apply-env-var-changes)
    :error-fn (partial error/generic-error-handler [main-cmd "env-vars" "apply-changes"])    :desc "Command for triggering an AWS Elastic Beanstalk environment restart. In order to update the environment variables in AWS Elastic Beanstalk, the environment has to be restarted. This can be done automatically by AWS (deploying a new application version...), using the AWS Console, or by running this command."
    :spec (help/with-help-spec
            [project-name-spec
             environment-spec
             region-spec])}

   ;; SSL manager
   {:cmds ["ssl" "create-and-upload-self-signed-certificate"]
    :fn (partial generic-handler-wrapper ssl/create-and-upload-self-signed-certificate)
    :error-fn (partial error/generic-error-handler [main-cmd "ssl" "create-and-upload-self-signed-certificate"])    :desc "Command for creating and uploading a self-signed certificate to AWS Certificate Manager."
    :spec (help/with-help-spec
            [region-spec])}

   ;; Cognito
   {:cmds ["cognito" "create-user"]
    :fn cognito-create-user-handler
    :error-fn (partial error/generic-error-handler [main-cmd "cognito" "create-user"])
    :desc "Create user in the specified AWS Cognito User Pool."
    :spec (help/with-help-spec
            [user-pool-id-spec
             username-spec
             [:temporary-password
              {:alias :p
               :desc "Temporary password assigned to the user. It will have to be changed on first login."}]
             [:attributes
              {:alias :a
               :coerce []
               :desc "User attributes in the form of 'ATTRIBUTE1=VAL1 ATTRIBUTE2=VAL2'"}]
             region-spec])}
   {:cmds ["cognito" "set-user-password"]
    :fn (partial generic-handler-wrapper cognito/admin-set-user-password)
    :error-fn (partial error/generic-error-handler [main-cmd "cognito" "set-user-password"])
    :desc "Change the password of the specified user."
    :spec (help/with-help-spec
            [user-pool-id-spec
             username-spec
             (update password-spec 1 (fn [m] (assoc m :desc "New password to be assigned to the user.")))
             [:temporary?
              {:alias :t
               :coerce :boolean
               :desc "Whether the new password is temporary. If so, it will have to be changed on first login."}]
             region-spec])}
   {:cmds ["cognito" "get-id-token"]
    :fn (partial generic-handler-wrapper cognito/get-id-token)
    :error-fn (partial error/generic-error-handler [main-cmd "cognito" "get-id-token"])
    :desc "Get an OpenID Connect Identity token for the specified user."
    :spec (help/with-help-spec
            [user-pool-id-spec
             [:client-id
              {:alias :c
               :require true
               :desc "Client ID to use to get the Identity token."}]
             username-spec
             (update password-spec 1 (fn [m] (assoc m :desc "Password of the AWS Cognito account.")))
             region-spec])}

   ;; RDS
   {:cmds ["rds" "start-port-forwarding-session"]
    :fn (partial generic-handler-wrapper rds/start-port-forwarding-session)
    :error-fn (partial error/generic-error-handler [main-cmd "rds" "start-port-forwarding-session"])
    :desc "Execute command to start a port forwarding session to a RDS instance. \nIn order to use this command you will need to have the AWS CLI installed, and the `AWS Session Manager plugin` for the mentioned tool."
    :spec (help/with-help-spec
            [project-name-spec
             environment-spec
             [:local-port
              {:alias :lp
               :require true
               :desc "Local port forwarded to the remote RDS instance."}]
             region-spec])}

   ;; Help
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) main-cmd))

(defn main [args]
  (cli/dispatch (cli-cmd-table) args))
