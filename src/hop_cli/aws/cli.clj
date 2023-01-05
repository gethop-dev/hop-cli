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

(defn- generic-handler-wrapper
  [handler-fn {:keys [opts]}]
  (pprint (handler-fn opts)))

(defn- cognito-create-user-handler
  [{:keys [opts]}]
  (let [parsed-opts (update opts :attributes util/cli-stdin-map->map)]
    (pprint (cognito/admin-create-user parsed-opts))))

(declare print-help-handler)

(defn- cli-cmd-table
  []
  [;; Environment Variable manager
   {:cmds ["env-vars" "sync"]
    :fn (partial generic-handler-wrapper env-vars/sync-env-vars)
    :error-fn error/generic-error-handler
    :desc "Command for synchronizing local environment variables with the remote environment variables storage in AWS SSM Parameter Store."
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e
            :desc "Target environment to update. E.g., prod, test"
            :require true}
           :file
           {:alias :f :require true}
           :kms-key-alias
           {:alias :k :require true
            :desc "Alias of the AWS KMS Key that will be used to encrypt the environment variables."}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   {:cmds ["env-vars" "download"]
    :fn (partial generic-handler-wrapper env-vars/download-env-vars)
    :error-fn error/generic-error-handler
    :desc "Command for downloading the environment variables from AWS SSM Parameter Store into a file."
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e
            :desc "Environment from where the variables will be obtained. E.g., prod, test"
            :require true}
           :file
           {:alias :f
            :desc "Path where the environment variables will be saved to."
            :require true}
           :kms-key-alias
           {:alias :k :require true
            :desc "Alias of the AWS KMS Key that will be used to decrypt the environment variables."}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   {:cmds ["env-vars" "apply-changes"]
    :fn (partial generic-handler-wrapper env-vars/apply-env-var-changes)
    :error-fn error/generic-error-handler
    :desc "Command for triggering an AWS Elastic Beanstalk environment restart. In order to update the environment variables in AWS Elastic Beanstalk, the environment has to be restarted. This can be done automatically by AWS (deploying a new application version...), using the AWS Console, or by running this command."
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e :require true}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   ;; SSL manager
   {:cmds ["ssl" "create-and-upload-self-signed-certificate"]
    :fn (partial generic-handler-wrapper ssl/create-and-upload-self-signed-certificate)
    :error-fn error/generic-error-handler
    :desc "Command for creating and uploading a self-signed certificate to AWS Certificate Manager."
    :spec {:region
           {:alias :r :require false
            :desc "Region"}}}

   ;; Cognito
   {:cmds ["cognito" "create-user"]
    :fn cognito-create-user-handler
    :error-fn error/generic-error-handler
    :desc "Create user in the specified AWS Cognito User Pool."
    :spec {:user-pool-id
           {:alias :up :require true}
           :username
           {:alias :u :require true
            :desc "Username or email"}
           :attributes
           {:alias :a
            :coerce []
            :desc "User attributes in the form of `ATTRIBUTE1=VAL1 ATTRIBUTE2=VAL2`"}
           :temporary-password
           {:alias :p}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   {:cmds ["cognito" "set-user-password"]
    :fn (partial generic-handler-wrapper cognito/admin-set-user-password)
    :error-fn error/generic-error-handler
    :desc "Change the password of the specified user."
    :spec {:user-pool-id
           {:alias :up :require true}
           :username
           {:alias :u :require true
            :desc "Username or email"}
           :password
           {:alias :p :require true}
           :temporary?
           {:alias :t}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   {:cmds ["cognito" "get-id-token"]
    :fn (partial generic-handler-wrapper cognito/get-id-token)
    :error-fn error/generic-error-handler
    :desc "Get OIDC identity token for the specified user."
    :spec {:user-pool-id
           {:alias :up :require true}
           :client-id
           {:alias :c :require true}
           :username
           {:alias :u :require true
            :desc "Username or email"}
           :password
           {:alias :p :require true}
           :region
           {:alias :r :require false
            :desc "Region"}}}
   ;; RDS
   {:cmds ["rds" "start-port-forwarding-session"]
    :fn (partial generic-handler-wrapper rds/start-port-forwarding-session)
    :error-fn error/generic-error-handler
    :desc "Execute command to start a port forwarding session to a RDS instance. \nIn order to use this command you will need to have installed `awscli` and the `AWS Session Manager plugin` for the mentioned tool."
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e :require true}
           :local-port
           {:alias :lp :require true
            :desc "Local port"}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   ;; Help
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) "aws"))

(defn main [args]
  (cli/dispatch (cli-cmd-table) args))
