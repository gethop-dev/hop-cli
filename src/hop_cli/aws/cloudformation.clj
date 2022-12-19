;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.cloudformation
  (:require [babashka.fs :as fs]
            [clojure.data :as data]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hop-cli.aws.api.cloudformation :as api.cf]
            [hop-cli.aws.api.s3 :as api.s3]
            [hop-cli.util.thread-transactions :as tht]))

(def ^:const cloudformation-template-ext
  ".yaml")

(defn compose-path
  [& args]
  (str/join "/" args))

(defn last-of-path
  [path]
  (-> path
      (str/split #"/")
      last))

(defn compose-file-key
  [file directory-path]
  (let [file-name (fs/file-name file)
        parent-dir (last-of-path (.getParent file))
        source-dir (last-of-path directory-path)]
    (if (= parent-dir source-dir)
      file-name
      (compose-path parent-dir file-name))))

(defn template-file?
  [file]
  (and
   (.isFile file)
   (str/ends-with? (fs/file-name file) cloudformation-template-ext)))

(defn upload-files
  [{:keys [directory-path] :as config} {:keys [bucket-name]}]
  (let [directory-file (fs/file directory-path)
        files (->> directory-file
                   file-seq
                   (filter template-file?))]
    (keep (fn [file]
            (api.s3/put-object config {:bucket-name bucket-name
                                       :key (compose-file-key file (str directory-file))
                                       :body (io/input-stream file)}))
          files)))

(defn bucket-exists?
  [config bucket-name]
  (:success? (api.s3/head-bucket config {:bucket-name bucket-name})))

(defn update-templates
  [{:keys [bucket-name] :as config}]
  (->
   [{:txn-fn
     (fn txn-1-create-bucket
       [_]
       (if (bucket-exists? config bucket-name)
         {:success? true
          :bucket-name bucket-name}
         (let [result (api.s3/create-bucket config {:bucket-name bucket-name})]
           (if-not (:success? result)
             result
             {:success? true
              :bucket-name bucket-name}))))
     :rollback-fn
      ;;FIXME This will fail if bucket is not empty. We need a strategy for it.
     (fn rollback-fn-1-delete-bucket
       [{:keys [bucket-name] :as prev-result}]
       (let [result (api.s3/delete-bucket config bucket-name)]
         (when-not (:success? result)
           (println "An error has occurred when deleting a bucket."))
         (dissoc prev-result :bucket-name)))}
    {:txn-fn
     (fn txn-2-upload-files
       [{:keys [bucket-name]}]
       (let [results (upload-files config {:bucket-name bucket-name})]
         (if-not (every? :success? results)
           {:success? false
            :bucket-name bucket-name
            :error-details results}
           {:success? true})))}]
   (tht/thread-transactions {})))

(defn- build-available-parameters
  [{:keys [project-name environment parameters s3-bucket-name]}]
  (cond-> parameters
    project-name
    (assoc :ProjectName project-name)

    environment
    (assoc :Environment environment)

    s3-bucket-name
    (assoc :TemplateBucketURL (api.cf/get-template-bucket-url s3-bucket-name))))

(defn- ensure-template-params
  [config]
  (let [result (api.cf/get-template-summary config)]
    (if-not (:success? result)
      {:success? false
       :reason :could-not-get-template-summary
       :error-details result}
      (let [available-parameters (build-available-parameters config)
            available-parameter-keys (set (keys available-parameters))
            template-parameters (get-in result [:template-summary :parameters])
            required-parameter-keys (->> template-parameters
                                         (filter :required?)
                                         (map :key)
                                         (set))
            all-parameter-keys  (set (map :key template-parameters))
            param-diff (data/diff required-parameter-keys available-parameter-keys)]
        (if (seq (get param-diff 0))
          {:success? false
           :reason :required-parameter-missing
           :error-details {:missing-keys (get param-diff 0)}}
          {:success? true
           :parameters (select-keys available-parameters all-parameter-keys)})))))

(defn- create-stack*
  [config]
  (let [result (ensure-template-params config)]
    (if (:success? result)
      (api.cf/create-stack (assoc config :parameters (:parameters result)))
      result)))

(defn- get-dependee-outputs
  [{:keys [dependee-stack-names]}]
  (let [describe-stack-results
        (map #(api.cf/describe-stack {:stack-name %}) dependee-stack-names)]
    (if-not (every? :success? describe-stack-results)
      {:success? false
       :error-details (remove :success? describe-stack-results)}
      {:success? true
       :outputs
       (->> describe-stack-results
            (map (comp :outputs :stack))
            (reduce merge {}))})))

(defn create-stack
  [{:keys [dependee-stack-names] :as config}]
  (if-not (seq dependee-stack-names)
    (create-stack* config)
    (let [result (get-dependee-outputs config)]
      (if (:success? result)
        (create-stack* (update config :parameters merge (:outputs result)))
        {:success? false
         :reason :could-not-get-dependee-outputs
         :error-details result}))))

(defn describe-stack
  [opts]
  (api.cf/describe-stack opts))
