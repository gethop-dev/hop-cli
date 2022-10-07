(ns hop.aws.ssm.parameter-store
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [com.grzm.awyeah.client.api :as aws]))

(defonce ssm-client
  (aws/client {:api :ssm}))

(defn- api-name->name
  [api-name]
  (last (str/split api-name #"/")))

(defn- name->api-name
  [{:keys [project-name environment]} name]
  (format "/%s/%s/env-variables/%s" project-name environment name))

(def param-name-conversion
  {:Value :value
   :Name :name
   :Version :version
   :LastModifiedDate :updated-at})

(defn- api-param->param
  [api-param]
  (-> api-param
      (select-keys (keys param-name-conversion))
      (set/rename-keys param-name-conversion)
      (update :name api-name->name)))

(defn put-parameter
  [{:keys [project-name environment kms-key-id] :as config} opts {:keys [name value]}]
  (let [request {:Name (name->api-name config name)
                 :Value value
                 :Type "SecureString"
                 :Tier "Standard"
                 :KeyId kms-key-id}
        request (cond-> request
                  (:new? opts)
                  (assoc :Tags
                         [{:Key "project-name"
                           :Value project-name}
                          {:Key "environment"
                           :Value environment}])
                  (not (:new? opts))
                  (assoc :Overwrite true))
        opts {:op :PutParameter
              :request request}
        result (aws/invoke ssm-client opts)]
    (if (:Version result)
      {:success? true}
      {:success? false
       :error-details result})))

(defn put-parameters
  [config opts parameters]
  (let [results (map (partial put-parameter config opts) parameters)]
    (if (every? :success? results)
      {:success? true}
      {:success? false
       :error-details (filter (comp not :success?) results)})))

(defn get-parameters
  [{:keys [project-name environment]}]
  (loop [params []
         next-token nil]
    (let [request {:Path (format "/%s/%s/env-variables" project-name environment)
                   :WithDecryption true
                   :NextToken next-token}
          opts {:op :GetParametersByPath
                :request request}
          result (aws/invoke ssm-client opts)]
      (if-let [new-params (:Parameters result)]
        (let [all-params (concat params (map api-param->param new-params))]
          (if-let [next-token (:NextToken result)]
            (recur all-params next-token)
            {:success? true
             :params all-params}))
        {:success? false
         :error-details {:result result
                         :succesfully-got-params (count params)}}))))

(defn delete-parameters
  [config parameters]
  (loop [params parameters
         deleted-params []
         invalid-params []]
    (if (empty? params)
      {:success? true
       :deleted-params deleted-params
       :invalid-params invalid-params}
      (let [params-to-delete (take 10 params)
            param-names (map #(name->api-name config (:name %)) params-to-delete)
            request {:Names param-names}
            opts {:op :DeleteParameters
                  :request request}
            result (aws/invoke ssm-client opts)]
        (if (:category result)
          {:success? false
           :error-details {:result result
                           :deleted-params deleted-params
                           :invalid-params invalid-params}}
          (recur (nthnext params 10) (:DeletedParameters result) (:InvalidParameters result)))))))
