(ns hop.aws.cli
  (:require [babashka.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [hop.aws.env-vars :as env-vars]
            [hop.aws.ssl :as ssl]
            [hop.aws.templates :as templates]))

(def common-cli-spec
  {:file {:alias :f
          :require true}
   :directory-path {:alias :d
                    :desc "Path to cloudformation templates directory"
                    :require true}
   :region {:alias :r
            :desc "AWS region in which the bucket should be created"
            :require true}
   :project-name {:alias :p
                  :require true}
   :environment {:alias :e
                 :require true}
   :kms-key-alias {:alias :k
                   :desc "Alias for the KMS key, or key id"
                   :require true}
   :stack-name {:alias :s
                :desc "AWS Cloudformation stack name."
                :require true}
   :s3-bucket-name {:alias :b
                    :desc "S3 Bucket name where Cloudformation templates are stored."
                    :require true}
   :master-template {:alias :m
                     :desc "Cloudformation stack master template filename."
                     :require true}
   :parameters {:alias :pa
                :coerce []
                :desc "Cloudformation stack parameters."}
   :dependee-stack-names {:alias :ds
                          :coerce []
                          :desc "The stack names that the new stack depends on."}
   :capability {:alias :c
                :coerce :keyword
                :desc "Stack capability."}})

(def cli-spec
  {:sync-env-vars (select-keys common-cli-spec [:project-name :environment :file :kms-key-alias])
   :download-env-vars (select-keys common-cli-spec [:project-name :environment :file :kms-key-alias])
   :apply-env-var-changes (select-keys common-cli-spec [:project-name :environment])
   :create-cf-templates-bucket (select-keys common-cli-spec [:directory-path :region])
   :create-cf-stack (-> common-cli-spec
                        (select-keys [:project-name :environment
                                      :stack-name :s3-bucket-name
                                      :master-template :parameters
                                      :dependee-stack-names :region
                                      :capability])
                        (assoc-in [:project-name :require] false)
                        (assoc-in [:environment :require] false))})

(defn- stdin-parameters->parameters
  [stdin-parameters]
  (reduce (fn [acc s]
            (let [[k v] (str/split s #"=" 2)]
              (assoc acc (keyword k) v)))
          {}
          stdin-parameters))

(defn- generic-error-handler
  [{:keys [msg spec]}]
  (println "An error has occurred:" msg)
  (println "Usage:")
  (println (cli/format-opts {:spec spec}))
  (System/exit 1))

(defn- sync-env-vars-handler
  [{:keys [opts]}]
  (pprint (env-vars/sync-env-vars opts)))

(defn- download-env-vars-handler
  [{:keys [opts]}]
  (pprint (env-vars/download-env-vars opts)))

(defn- apply-env-var-changes-handler
  [{:keys [opts]}]
  (pprint (env-vars/apply-env-var-changes-handler opts)))

(defn- update-cf-templates-handler
  [{:keys [opts]}]
  (pprint (templates/update-cf-templates opts)))

(defn- create-cf-stack
  [{:keys [opts]}]
  (let [parsed-opts (update opts :parameters stdin-parameters->parameters)]
    (pprint (templates/create-cf-stack parsed-opts))))

(defn- create-and-upload-self-signed-certificate-handler
  [_]
  (pprint (ssl/create-and-upload-self-signed-certificate)))

(declare print-help)

(defn- cli-cmd-table
  []
  [{:cmds ["sync-env-vars"]
    :fn sync-env-vars-handler
    :spec (get cli-spec :sync-env-vars)
    :error-fn generic-error-handler
    :desc "Synchronize local environment variables with AWS SSMPS"}
   {:cmds ["download-env-vars"]
    :fn download-env-vars-handler
    :spec (get cli-spec :download-env-vars)
    :error-fn generic-error-handler
    :desc "Download environment variables from AWS SSMPS"}
   {:cmds ["apply-env-var-changes"]
    :fn apply-env-var-changes-handler
    :spec (get cli-spec :apply-env-var-changes)
    :error-fn generic-error-handler
    :desc "Apply environment variables changes in a AWS Elasticbeanstalk environment"}
   {:cmds ["update-cf-templates"]
    :fn update-cf-templates-handler
    :spec (get cli-spec :create-cf-templates-bucket)
    :error-fn generic-error-handler
    :desc "Updates CF templates in the specified bucket. If the bucket doesn't exist it is created"}
   {:cmds ["create-cf-stack"]
    :fn create-cf-stack
    :spec (get cli-spec :create-cf-stack)
    :error-fn generic-error-handler
    :desc "Creates a Cloudformation stack"}
   {:cmds ["create-and-upload-self-signed-certificate"]
    :fn create-and-upload-self-signed-certificate-handler
    :spec {}
    :error-fn generic-error-handler
    :desc "Creates an uploads a SSL self-signed certificate to ACM"}
   {:cmds []
    :fn print-help}])

(defn- print-help
  [_]
  (let [max-cmd-len (reduce #(max %1 (-> %2 :cmds first count)) 0 (cli-cmd-table))]
    (println "Usage: aws-util <subcommand> <options>")
    (println)
    (println "Subcommands")
    (doseq [{:keys [cmds desc]} (cli-cmd-table)
            :when (seq cmds)
            :let [format-str (str "  %-" max-cmd-len "s  %s")
                  cmd (first cmds)]]
      (println (format format-str cmd desc)))))

(defn -main [& args]
  (cli/dispatch (cli-cmd-table) args))
