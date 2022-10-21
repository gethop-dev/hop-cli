(ns hop.aws.cloudformation.stack
  (:require [com.grzm.awyeah.client.api :as aws]))

(def cf-client (aws/client {:api :cloudformation}))

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

(defn- get-template-url
  [s3-bucket-name master-template]
  (format "https://%s.s3.amazonaws.com/%s" s3-bucket-name master-template))

(defn- get-template-bucket-url
  [s3-bucket-name]
  (format "https://%s.s3.amazonaws.com" s3-bucket-name))

(defn describe-stack
  [_ {:keys [stack-name]}]
  (let [request {:StackName stack-name}
        opts {:op :DescribeStacks
              :request request}
        result (aws/invoke cf-client opts)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :stack (-> result
                  :Stacks
                  first
                  (update :Outputs api-outputs->outputs))})))

(defn create-stack
  [{:keys [project-name environment stack-name parameters capability s3-bucket-name master-template]}]
  (let [template-bucket-url (get-template-bucket-url s3-bucket-name)
        request {:StackName stack-name
                 :Parameters (parameters->api-parameters (assoc parameters :TemplateBucketURL template-bucket-url))
                 :Capabilities (capability->api-capabilities capability)
                 :Tags (cond-> [{:Key "project-name" :Value project-name}]
                         (seq environment)
                         (conj {:Key "environment" :Value environment}))
                 :TemplateURL (get-template-url s3-bucket-name master-template)}
        opts {:op :CreateStack
              :request request}
        result (aws/invoke cf-client opts)]
    (if (:cognitect.anomalies/category result)
      {:success? false
       :error-details result}
      {:success? true
       :stack-id (:StackId result)})))
