(ns hop.aws.templates
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [hop.aws.cloudformation.stack :as cf.stack]
            [hop.aws.s3.bucket :as s3.bucket]
            [hop.aws.sts.identity :as sts.identity]
            [hop.aws.util.thread-transactions :as tht]))

(def ^:const cloudformation-templates-s3-prefix
  "cloudformation-templates-")

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
  (let [files (->> directory-path
                   fs/file
                   file-seq
                   (filter template-file?))]
    (keep (fn [file]
            (s3.bucket/put-object config {:bucket-name bucket-name
                                          :key (compose-file-key file directory-path)
                                          :body (.getPath file)}))
          files)))

(defn bucket-exists?
  [config bucket-name]
  (:success? (s3.bucket/head-bucket config {:bucket-name bucket-name})))

(defn create-cf-templates-bucket-handler
  [config]
  (->
    [{:txn-fn
      (fn txn-1-get-caller-identity
        [_]
        (let [result (sts.identity/get-caller-identity)]
          (if-not (:success? result)
            result
            {:success? true
             :caller-identity (:caller-identity result)})))}
     {:txn-fn
      (fn txn-2-create-bucket
        [{:keys [caller-identity]}]
        (let [caller-account-number (:Account caller-identity)
              bucket-name (str cloudformation-templates-s3-prefix caller-account-number)]
          (if (bucket-exists? config bucket-name)
            {:success? true
             :bucket-name bucket-name}
            (let [result (s3.bucket/create-bucket config {:bucket-name bucket-name})]
              (if-not (:success? result)
                result
                {:success? true
                 :bucket-name bucket-name})))))
      :rollback-fn
      ;;FIXME This will fail if bucket is not empty. We need a strategy for it.
      (fn rollback-fn-2-delete-bucket
        [{:keys [bucket-name] :as prev-result}]
        (let [result (s3.bucket/delete-bucket config bucket-name)]
          (when-not (:success? result)
            (println "An error has occurred when deleting a bucket."))
          (dissoc prev-result :bucket-name)))}
     {:txn-fn
      (fn txn-3-upload-files
        [{:keys [bucket-name]}]
        (let [results (upload-files config {:bucket-name bucket-name})]
          (if-not (every? :success? results)
            {:success? false
             :bucket-name bucket-name
             :error-details results}
            {:success? true})))}]
    (tht/thread-transactions {})))

(defn create-cf-stack
  [{:keys [dependee-stack-names parameters] :as config}]
  (if-not (seq dependee-stack-names)
    (cf.stack/create-stack config)
    (let [describe-stack-results (map #(cf.stack/describe-stack config {:stack-name %}) dependee-stack-names)]
      (if-not (every? :success? describe-stack-results)
        {:success? false
         :error-details (remove :success? describe-stack-results)}
        (let [stacks (map :stack describe-stack-results)
              new-stack-parameters (reduce (fn [acc {:keys [Outputs]}]
                                             (merge acc Outputs))
                                           parameters
                                           stacks)]
          (cf.stack/create-stack (assoc config :parameters new-stack-parameters)))))))
