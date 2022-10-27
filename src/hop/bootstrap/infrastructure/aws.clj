(ns hop.bootstrap.infrastructure.aws
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [hop.aws.ssl :as aws.ssl]
            [hop.aws.templates :as aws.templates]
            [hop.util.thread-transactions :as tht]))

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
             :dependee-stack-names ["hop-account"]
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :aws.project/stack-name
             :parameter-mapping
             {:aws.project.vpc/id :VpcId
              :aws.project.vpc/public-route-table-id :PublicRouteTable1Id
              :aws.project.vpc.subnet1/cidr :Subnet1CIDR
              :aws.project.vpc.subnet2/cidr :Subnet2CIDR
              :aws.project.elb/certificate-arn :ElbCertificateArn}}

   :dev-env {:master-template "local-environment.yaml"
             :dependee-stack-names ["hop-account" "hop-project"]
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :aws.environment.dev/stack-name
             :environment "dev"
             :parameter-mapping {}}

   :test-env {:master-template "cloud-environment.yaml"
              :dependee-stack-names ["hop-account" "hop-project"]
              :capability :CAPABILITY_NAMED_IAM
              :stack-name-kw :aws.environment.test/stack-name
              :environment "test"
              :parameter-mapping
              {:aws.environment.test/notifications-email :NotificationsEmail
               :aws.environment.test.database/version :DatabaseEngineVersion
               :aws.environment.test.database/password :DatabasePassword}}

   :prod-env {:master-template "cloud-environment.yaml"
              :dependee-stack-names ["hop-account" "hop-project"]
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
    (let [result (aws.templates/describe-stack {:stack-name stack-name})
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
  [config {:keys [parameter-mapping stack-name-kw] :as template-opts}]
  (let [stack-name (get config stack-name-kw)
        project-name (:project/name config)
        bucket-name (:aws/cloudformation-templates-bucket-name config)
        parameters (-> config
                       (select-keys (keys parameter-mapping))
                       (set/rename-keys parameter-mapping))
        opts (assoc template-opts
                    :project-name project-name
                    :parameters parameters
                    :stack-name stack-name
                    :s3-bucket-name bucket-name)
        _log (println (format "Provisioning cloudformation %s stack..." stack-name))
        result (aws.templates/create-cf-stack opts)]
    (if (:success? result)
      (wait-for-stack-completion stack-name)
      result)))

(defn provision-infrastructure
  [config]
  (->
   [{:txn-fn
     (fn upload-cloudformation-templates
       [_]
       (println (format "Uploading cloudformation templates to %s bucket..." (:aws/cloudformation-templates-bucket-name config)))
       (let [bucket-name (:aws/cloudformation-templates-bucket-name config)
             opts {:bucket-name bucket-name
                   :directory-path cfn-templates-path}
             result (aws.templates/update-cf-templates opts)]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-upload-cfn-templates
            :error-details result})))}
    {:txn-fn
     (fn provision-account
       [_]
       (if-not (get config :aws.account/enabled)
         {:success? true}
         (let [result (provision-cfn-stack config (:account cfn-templates))]
           (if (:success? result)
             {:success? true}
             {:success? false
              :reason :could-not-provision-account-cfn
              :error-details result}))))}
    {:txn-fn
     (fn create-and-upload-self-signed-certificate
       [_]
       (if (:aws.project.elb/certificate-arn config)
         (do
           (println "Skipping self-signed certificate upload.")
           {:success? true})
         (let [_log (println "Creating and uploading self-signed certificate...")
               result (aws.ssl/create-and-upload-self-signed-certificate)]
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
       (if-not (get config :aws.project/enabled)
         (do
           (println "Skipping cloudformation project type stack creation...")
           {:success? true
            :config config})
         (let [result (provision-cfn-stack config (:project cfn-templates))]
           (if (:success? result)
             {:success? true
              :config config}
             {:success? false
              :reason :could-not-provision-project-cfn
              :error-details result}))))}
    {:txn-fn
     (fn provision-dev-env
       [{:keys [config]}]
       (if-not (get config :aws.environment.dev/enabled)
         (do
           (println "Skipping cloudformation dev type stack creation...")
           {:success? true
            :config config})
         (let [result (provision-cfn-stack config (:dev-env cfn-templates))]
           (if (:success? result)
             {:success? true
              :config config}
             {:success? false
              :reason :could-not-provision-dev-env
              :error-details result}))))}
    {:txn-fn
     (fn provision-test-env
       [{:keys [config]}]
       (if-not (get config :aws.environment.test/enabled)
         (do
           (println "Skipping cloudformation test type stack creation...")
           {:success? true
            :config config})
         (let [result (provision-cfn-stack config (:test-env cfn-templates))]
           (if (:success? result)
             {:success? true
              :config config}
             {:success? false
              :reason :could-not-provision-test-env
              :error-details result}))))}
    {:txn-fn
     (fn provision-prod-env
       [{:keys [config]}]
       (if-not (get config :aws.environment.prod/enabled)
         (do
           (println "Skipping cloudformation prod type stack creation...")
           {:success? true})
         (let [result (provision-cfn-stack config (:prod-env cfn-templates))]
           (if (:success? result)
             {:success? true}
             {:success? false
              :reason :could-not-provision-prod-env

              :error-details result}))))}]
   (tht/thread-transactions {})))


(def ^:const foo-config
  {:project/name "hop"
   :aws/cloudformation-templates-bucket-name "cloudformation-templates-223330293187"
   :aws.account/enabled false
   :aws.account/stack-name "hop-account"
   :aws.project/enabled true
   :aws.project/stack-name "hop-project"
   :aws.account.vpc/cidr "172.31.0.0/16"
   :aws.account/resource-name-prefix "hop"
   :aws.project.vpc.subnet1/cidr "172.31.0.0/24"
   :aws.project.vpc.subnet2/cidr "172.31.1.0/24"
   :aws.environment.test/enabled true
   :aws.environment.test/stack-name "hop-test-env"
   :aws.environment.test.database/version "12"
   :aws.environment.test.database/password "password"
   :aws.environment.test/notification-email "devops@magnet.coop"
   :aws.environment.prod/enabled true
   :aws.environment.prod/stack-name "hop-prod-env"
   :aws.environment.prod.database/version "12"
   :aws.environment.prod.database/password "password"
   :aws.environment.prod/notification-email "devops@magnet.coop"})