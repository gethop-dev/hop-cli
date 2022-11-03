(ns hop-cli.aws.cli
  (:require [babashka.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [hop-cli.aws.cloudformation :as cloudformation]
            [hop-cli.aws.env-vars :as env-vars]
            [hop-cli.aws.ssl :as ssl]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(defn- stdin-parameters->parameters
  [stdin-parameters]
  (reduce (fn [acc s]
            (let [[k v] (str/split s #"=" 2)]
              (assoc acc (keyword k) v)))
          {}
          stdin-parameters))

(defn- generic-handler-wrapper
  [handler-fn {:keys [opts]}]
  (pprint (handler-fn opts)))

(defn- cf-create-stack-handler
  [{:keys [opts]}]
  (let [parsed-opts (update opts :parameters stdin-parameters->parameters)]
    (pprint (cloudformation/create-stack parsed-opts))))

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
            :desc "Alias or name of the KMS key"}}}

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
            :desc "Alias or name of the KMS key"}}}

   {:cmds ["env-vars" "apply-changes"]
    :fn (partial generic-handler-wrapper env-vars/apply-env-var-changes)
    :error-fn error/generic-error-handler
    :desc "Apply environment variables changes in a AWS Elasticbeanstalk environment"
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e :require true}}}

   ;; Cloudformation
   {:cmds ["cloudformation" "update-templates"]
    :fn (partial generic-handler-wrapper cloudformation/update-templates)
    :error-fn error/generic-error-handler
    :desc "Updates CF templates in the specified bucket. If the bucket doesn't exist it is created"
    :spec {:directory-path
           {:alias :d :require true
            :desc "Path to cloudformation templates directory"}}}

   {:cmds ["cloudformation" "create-stack"]
    :fn cf-create-stack-handler
    :error-fn error/generic-error-handler
    :desc "Creates a Cloudformation stack"
    :spec {:project-name
           {:alias :p :require true}
           :environment
           {:alias :e :require true}
           :stack-name
           {:alias :s
            :require true}
           :s3-bucket-name
           {:alias :b
            :desc "S3 Bucket name where Cloudformation templates are stored."
            :require true}
           :master-template
           {:alias :m
            :desc "Master template filename."
            :require true}
           :parameters
           {:alias :pa
            :coerce []
            :desc "Stack parameters."}
           :dependee-stack-names
           {:alias :ds
            :coerce []
            :desc "The stack names that the new stack depends on."}
           :capability
           {:alias :c
            :coerce :keyword
            :desc "Stack capability."}}}

   ;; SSL manager
   {:cmds ["ssl" "create-and-upload-self-signed-certificate"]
    :fn (partial generic-handler-wrapper ssl/create-and-upload-self-signed-certificate)
    :error-fn error/generic-error-handler
    :desc "Creates an uploads a SSL self-signed certificate to ACM"
    :spec {}}

   ;; Help
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) "aws"))

(defn main [args]
  (cli/dispatch (cli-cmd-table) args))
