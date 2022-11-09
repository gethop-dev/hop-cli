(ns hop-cli.bootstrap.infrastructure.aws
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [hop-cli.aws.cloudformation :as aws.cloudformation]
            [hop-cli.aws.ssl :as aws.ssl]
            [hop-cli.util.thread-transactions :as tht]))

(def ^:const cfn-templates-path
  (io/resource "infrastructure/cloudformation-templates"))

(def cfn-templates
  {:account {:master-template "account.yaml"
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :aws.account/stack-name
             :parameter-mapping
             {:aws.account.vpc/cidr :VpcCIDR
              :aws.account/resource-name-prefix :ResourceNamePrefix}}

   :project {:master-template "project.yaml"
             :dependee-stack-kws [:aws.account/stack-name]
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :aws.project/stack-name
             :parameter-mapping
             {:aws.project.vpc/id :VpcId
              :aws.project.vpc/public-route-table-id :PublicRouteTable1Id
              :aws.project.vpc.subnet-1/cidr :Subnet1CIDR
              :aws.project.vpc.subnet-2/cidr :Subnet2CIDR
              :aws.project.elb/certificate-arn :ElbCertificateArn}}

   :dev-env {:master-template "local-environment.yaml"
             :dependee-stack-kws [:aws.account/stack-name :aws.project/stack-name]
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :aws.environment.dev/stack-name
             :environment "dev"
             :parameter-mapping {}}

   :test-env {:master-template "cloud-environment.yaml"
              :dependee-stack-kws [:aws.account/stack-name :aws.project/stack-name]
              :capability :CAPABILITY_NAMED_IAM
              :stack-name-kw :aws.environment.test/stack-name
              :environment "test"
              :parameter-mapping
              {:aws.environment.test/notifications-email :NotificationsEmail
               :aws.environment.test.database/version :DatabaseEngineVersion
               :aws.environment.test.database/password :DatabasePassword}}

   :prod-env {:master-template "cloud-environment.yaml"
              :dependee-stack-kws [:aws.account/stack-name :aws.project/stack-name]
              :capability :CAPABILITY_NAMED_IAM
              :stack-name-kw :aws.environment.prod/stack-name
              :environment "prod"
              :parameter-mapping
              {:aws.environment.prod/notifications-email :NotificationsEmail
               :aws.environment.prod.database/version :DatabaseEngineVersion
               :aws.environment.prod.database/password :DatabasePassword}}})

(defn wait-for-stack-completion
  [stack-name]
  (loop []
    (let [result (aws.cloudformation/describe-stack {:stack-name stack-name})
          status (get-in result [:stack :status])]
      (cond
        (= status :CREATE_IN_PROGRESS)
        (do
          (println (format "%s stack creation in progress. Retrying in 10 seconds..." stack-name))
          (Thread/sleep 10000)
          (recur))

        (= status :CREATE_COMPLETE)
        {:success? true}

        :else
        {:success? false
         :error-details result}))))

(defn- provision-cfn-stack
  [config {:keys [parameter-mapping stack-name-kw dependee-stack-kws] :as template-opts}]
  (let [stack-name (get config stack-name-kw)
        dependee-stack-names (mapv #(get config %)
                                   dependee-stack-kws)
        project-name (:project/name config)
        bucket-name (:aws.cloudformation/template-bucket-name config)
        parameters (-> config
                       (select-keys (keys parameter-mapping))
                       (set/rename-keys parameter-mapping))
        opts (assoc template-opts
                    :project-name project-name
                    :parameters parameters
                    :stack-name stack-name
                    :s3-bucket-name bucket-name
                    :dependee-stack-names dependee-stack-names)
        _log (println (format "Provisioning cloudformation %s stack..." stack-name))
        result (aws.cloudformation/create-stack opts)]
    (if (:success? result)
      (wait-for-stack-completion stack-name)
      result)))

(defn provision-initial-infrastructure
  [config]
  (->
   [{:txn-fn
     (fn upload-cloudformation-templates
       [_]
       (println (format "Uploading cloudformation templates to %s bucket..." (:aws/cloudformation-templates-bucket-name config)))
       (let [bucket-name (:aws.cloudformation/template-bucket-name config)
             opts {:bucket-name bucket-name
                   :directory-path cfn-templates-path}
             result (aws.cloudformation/update-templates opts)]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-upload-cfn-templates
            :error-details result})))}
    {:txn-fn
     (fn provision-account
       [_]
       (let [result (provision-cfn-stack config (:account cfn-templates))]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-provision-account-cfn
            :error-details result})))}
    {:txn-fn
     (fn create-and-upload-self-signed-certificate
       [_]
       (if (:aws.project.elb/certificate-arn config)
         (do
           (println "Skipping self-signed certificate upload.")
           {:success? true})
         (let [_log (println "Creating and uploading self-signed certificate...")
               result (aws.ssl/create-and-upload-self-signed-certificate {})]
           (if (:success? result)
             (let [certificate-arn (:certificate-arn result)
                   updated-config (assoc config
                                         :aws.project.elb/certificate-arn certificate-arn)]
               {:success? true
                :config updated-config})
             {:success? false
              :reason :could-not-create-and-upload-self-signed-certificate
              :error-details result}))))}
    {:txn-fn
     (fn provision-project
       [{:keys [config]}]
       (let [result (provision-cfn-stack config (:project cfn-templates))]
         (if (:success? result)
           {:success? true
            :config config}
           {:success? false
            :reason :could-not-provision-project-cfn
            :error-details result})))}
    {:txn-fn
     (fn provision-dev-env
       [{:keys [config]}]
       (let [result (provision-cfn-stack config (:dev-env cfn-templates))]
         (if (:success? result)
           {:success? true
            :config config}
           {:success? false
            :reason :could-not-provision-dev-env
            :error-details result})))}
    {:txn-fn
     (fn provision-test-env
       [{:keys [config]}]
       (let [result (provision-cfn-stack config (:test-env cfn-templates))]
         (if (:success? result)
           {:success? true
            :config config}
           {:success? false
            :reason :could-not-provision-test-env
            :error-details result})))}]
   (tht/thread-transactions {})))

(defn provision-prod-infrastructure
  [config]
  (provision-cfn-stack config (:prod-env cfn-templates)))
