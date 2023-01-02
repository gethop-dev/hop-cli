;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.cloudformation
  (:require [clojure.set :as set]
            [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn- parameters->api-parameters
  [parameters]
  (map (fn [[k v]]
         {:ParameterKey (name k)
          :ParameterValue v})
       parameters))

(defn- api-outputs->outputs
  [outputs]
  (reduce (fn [acc {:keys [OutputKey OutputValue]}]
            (assoc acc (keyword OutputKey) OutputValue))
          {}
          outputs))

(defn- capability->api-capabilities
  [capability]
  (vector (name capability)))

(defn get-template-bucket-url
  [s3-bucket-name]
  (format "https://%s.s3.amazonaws.com" s3-bucket-name))

(defn- get-template-url
  [s3-bucket-name master-template]
  (format "https://%s.s3.amazonaws.com/%s" s3-bucket-name master-template))

(def ^:const stack-field-mapping
  {:Outputs :outputs
   :StackStatus :status
   :StackStatusReason :status-reason})

(defn- api-stack->stack
  [api-stack]
  (-> api-stack
      (select-keys (keys stack-field-mapping))
      (set/rename-keys stack-field-mapping)
      (update :outputs api-outputs->outputs)
      (update :status keyword)))

(defn describe-stack
  [{:keys [stack-name] :as opts}]
  (let [cf-client (aws.client/gen-client :cloudformation opts)
        request {:StackName stack-name}
        args {:op :DescribeStacks
              :request request}
        result (aws/invoke cf-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      (let [api-stack (-> result :Stacks first)]
        {:success? true
         :stack (api-stack->stack api-stack)}))))

(defn create-stack
  [{:keys [project-name environment stack-name parameters capability s3-bucket-name master-template] :as opts}]
  (let [cf-client (aws.client/gen-client :cloudformation opts)
        request {:StackName stack-name
                 :Parameters (parameters->api-parameters parameters)
                 :Capabilities (capability->api-capabilities capability)
                 :Tags (cond-> []
                         (seq project-name)
                         (conj {:Key "project-name" :Value project-name})
                         (seq environment)
                         (conj {:Key "environment" :Value environment}))
                 :TemplateURL (get-template-url s3-bucket-name master-template)}
        args {:op :CreateStack
              :request request}
        result (aws/invoke cf-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :stack-id (:StackId result)})))

(defn- api-template-summary->template-summary
  [{:keys [Parameters Capabilities]}]
  {:capabilities (map keyword Capabilities)
   :parameters
   (map
    (fn [{:keys [ParameterKey DefaultValue]}]
      {:key (keyword ParameterKey)
       :required? (nil? DefaultValue)})
    Parameters)})

(defn get-template-summary
  [{:keys [s3-bucket-name master-template] :as opts}]
  (let [cf-client (aws.client/gen-client :cloudformation opts)
        template-url (get-template-url s3-bucket-name master-template)
        request {:TemplateURL template-url}
        args {:op :GetTemplateSummary
              :request request}
        result (aws/invoke cf-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :template-summary (api-template-summary->template-summary result)
       :stack-id (:StackId result)})))

(def stack-event-field-mapping
  {:ResourceStatusReason :resource-status-reason
   :ResourceStatus :resource-status
   :ResourceType :resource-type
   :PhysicalResourceId :physical-resource-id
   :LogicalResourceId :logical-resource-id
   :Timestamp :timestamp})

(defn- api-stack-event->stack-event
  [api-stack-event]
  (-> api-stack-event
      (select-keys (keys stack-event-field-mapping))
      (set/rename-keys stack-event-field-mapping)
      (update :resource-status keyword)))

(defn describe-stack-events
  [{:keys [stack-name] :as opts}]
  (let [cf-client (aws.client/gen-client :cloudformation opts)
        request {:StackName stack-name}
        args {:op :DescribeStackEvents
              :request request}
        result (aws/invoke cf-client args)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :stack-events (map api-stack-event->stack-event (:StackEvents result))})))
