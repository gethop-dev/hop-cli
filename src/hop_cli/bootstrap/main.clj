;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.main
  (:require [clojure.string :as str]
            [hop-cli.bootstrap.infrastructure :as infrastructure]
            [hop-cli.bootstrap.infrastructure.aws]
            [hop-cli.bootstrap.profile :as profile]
            [hop-cli.bootstrap.settings-reader :as sr]
            [hop-cli.bootstrap.util :as bp.util]
            [hop-cli.util.thread-transactions :as tht]))

(defn bootstrap-hop
  [{:keys [settings-file-path target-project-dir environments]}]
  (->
   [{:txn-fn
     (fn read-settings [_]
       (println "Reading settings.edn...")
       (let [result (sr/read-settings settings-file-path)]
         (if (:success? result)
           {:success? true
            :settings
            (-> (:settings result)
                (assoc-in [:project :target-dir] target-project-dir)
                (assoc-in [:project :environments] environments))}
           {:success? false
            :reason :could-not-read-settings
            :error-details result})))}
    {:txn-fn
     (fn provision-infrastructure
       [{:keys [settings]}]
       (println "Provisioning infrastructure")
       (let [result (infrastructure/provision-initial-infrastructure settings)]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :failed-to-provision-infrastructure
            :error-details result})))}
    {:txn-fn
     (fn execute-profiles
       [{:keys [settings]}]
       (println "Generating project...")
       (let [result (profile/execute-profiles! settings)]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :could-not-execute-profiles
            :error-details result})))}
    {:txn-fn
     (fn save-environment-variables
       [{:keys [settings]}]
       (println "Saving environment variables...")
       (let [result (infrastructure/save-environment-variables settings)]
         (if (:success? result)
           {:success? true
            :settings settings}
           {:success? false
            :reason :could-not-save-env-variables
            :error-details result})))}
    {:txn-fn
     (fn post-installation-messages
       [{:keys [settings]}]
       (println "Project generation finished. Now follow these manual steps to complete the bootstrap.")
       (doseq [environment environments
               :let [messages (bp.util/get-settings-value settings [:project :post-installation-messages environment])]
               :when (seq messages)]
         (println (format "\nSteps to complete the %s environment setup" (str/upper-case (name environment))))
         (doseq [[n msg] (map-indexed vector messages)]
           (println (format "Step #%s" n))
           (println msg)))
       {:success? true})}]
   (tht/thread-transactions {})))
