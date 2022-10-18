(ns hop.aws.cli
  (:require [babashka.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [hop.aws.env-vars :as env-vars]
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
                   :require true}})

(def cli-spec
  {:sync-env-vars common-cli-spec
   :download-env-vars common-cli-spec
   :apply-env-var-changes (select-keys common-cli-spec [:project-name :environment])
   :create-cf-templates-bucket (select-keys common-cli-spec [:directory-path :region])})

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

(defn- create-cf-templates-bucket-handler
  [{:keys [opts]}]
  (pprint (templates/create-cf-templates-bucket-handler opts)))

(def cli-table
  [{:cmds ["sync-env-vars"]
    :fn sync-env-vars-handler
    :spec (get cli-spec :sync-env-vars)
    :error-fn generic-error-handler}
   {:cmds ["download-env-vars"]
    :fn download-env-vars-handler
    :spec (get cli-spec :download-env-vars)
    :error-fn generic-error-handler}
   {:cmds ["apply-env-var-changes"]
    :fn apply-env-var-changes-handler
    :spec (get cli-spec :apply-env-var-changes)
    :error-fn generic-error-handler}
   {:cmds ["create-cf-templates-bucket"]
    :fn create-cf-templates-bucket-handler
    :spec (get cli-spec :create-cf-templates-bucket)
    :error-fn generic-error-handler}])

(defn -main [& args]
  (cli/dispatch cli-table args {:coerce {:depth :long}}))
