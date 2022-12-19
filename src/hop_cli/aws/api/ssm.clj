;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.ssm
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
   :Name :name})

(defn- api-param->param
  [api-param]
  (-> api-param
      (select-keys (keys param-name-conversion))
      (set/rename-keys param-name-conversion)
      (update :name api-name->name)))

(defn put-parameter
  [{:keys [project-name environment kms-key-alias] :as config} opts {:keys [name value]}]
  (let [request {:Name (name->api-name config name)
                 :Value value
                 :Type "SecureString"
                 :Tier "Standard"
                 :KeyId kms-key-alias}
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

(defn put-parameters*
  [config opts parameters]
  (loop [pending-params parameters
         completed-results []]
    (if (empty? pending-params)
      completed-results
      (let [result (put-parameter config opts (first pending-params))]
        (if (and
             (not (:success? result))
             (= "ThrottlingException" (get-in result [:error-details :__type])))
          (do
            (println "SSM Rate limit exceeded. Retrying in 3s...")
            (Thread/sleep 3000)
            (recur pending-params completed-results))
          (recur (rest pending-params) (conj completed-results result)))))))

(defn put-parameters
  [config opts parameters]
  (let [results (put-parameters* config opts parameters)]
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
