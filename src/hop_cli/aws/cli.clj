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

(defn- start-rds-port-forwarding-session-handler
  [{:keys [opts]}]
  (let [result (rds/start-port-forwarding-session opts)]
    (if (:success? result)
      (println (:command result))
      (pprint result))))

(declare print-help-handler)

(defn- cli-cmd-table
  []
  [;; Environment Variable manager
   {:cmds ["env-vars" "sync"]
    :fn (partial generic-handler-wrapper env-vars/sync-env-vars)
    :error-fn error/generic-error-handler
    :desc "Synchronize local environment variables with AWS SSMPS"
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e :require true}
           :file
           {:alias :f :require true}
           :kms-key-alias
           {:alias :k :require true
            :desc "Alias or name of the KMS key"}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   {:cmds ["env-vars" "download"]
    :fn (partial generic-handler-wrapper env-vars/download-env-vars)
    :error-fn error/generic-error-handler
    :desc "Download environment variables from AWS SSMPS"
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e :require true}
           :file
           {:alias :f :require true}
           :kms-key-alias
           {:alias :k :require true
            :desc "Alias or name of the KMS key"}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   {:cmds ["env-vars" "apply-changes"]
    :fn (partial generic-handler-wrapper env-vars/apply-env-var-changes)
    :error-fn error/generic-error-handler
    :desc "Apply environment variables changes in a AWS Elasticbeanstalk environment"
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
    :desc "Creates an uploads a SSL self-signed certificate to ACM"
    :spec {:region
           {:alias :r :require false
            :desc "Region"}}}

   ;; Cognito
   {:cmds ["cognito" "create-user"]
    :fn cognito-create-user-handler
    :error-fn error/generic-error-handler
    :desc "Create a user in the specified Cognito identity pool"
    :spec {:user-pool-id
           {:alias :up :require true}
           :username
           {:alias :u :require true
            :desc "Username or email"}
           :attributes
           {:alias :a
            :coerce []
            :desc "Attributes in the form of param1=value1 param2=value2..."}
           :temporary-password
           {:alias :p}
           :region
           {:alias :r :require false
            :desc "Region"}}}

   {:cmds ["cognito" "set-user-password"]
    :fn (partial generic-handler-wrapper cognito/admin-set-user-password)
    :error-fn error/generic-error-handler
    :desc "Change the password of the user"
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
    :desc "Get ID token for the user"
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
    :fn start-rds-port-forwarding-session-handler
    :error-fn error/generic-error-handler
    :desc "Execute command to start a port forwarding session to a RDS instance"
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
