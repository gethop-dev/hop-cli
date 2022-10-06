(ns hop.aws.cli
  (:require [babashka.cli :as cli]
            [hop.aws.env-vars :as env-vars]))

(def cli-spec
  {:file {:alias :f
          :require true}
   :project-name {:alias :p
                  :require true}
   :environment {:alias :e
                 :require true}
   :kms-key-id {:alias :k
                :require true}})

(defn- generic-error-handler
  [{:keys [msg]}]
  (println "An error has occurred:" msg)
  (println "Usage:")
  (println (cli/format-opts {:spec cli-spec}))
  (System/exit 1))

(defn- sync-env-vars-handler
  [{:keys [opts]}]
  (println (env-vars/sync-env-vars opts)))

(defn- download-env-vars-handler
  [{:keys [opts]}]
  (println (env-vars/download-env-vars opts)))

(def cli-table
  [{:cmds ["sync-env-vars"]
    :fn sync-env-vars-handler
    :spec cli-spec
    :error-fn generic-error-handler}
   {:cmds ["download-env-vars"]
    :fn download-env-vars-handler
    :spec cli-spec
    :error-fn generic-error-handler}])

(defn -main [& args]
  (cli/dispatch cli-table args {:coerce {:depth :long}}))
