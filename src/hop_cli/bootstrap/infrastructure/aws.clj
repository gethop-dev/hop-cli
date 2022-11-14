(ns hop-cli.bootstrap.infrastructure.aws
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [hop-cli.aws.api.ssm :as api.ssm]
            [hop-cli.aws.cloudformation :as aws.cloudformation]
            [hop-cli.aws.ssl :as aws.ssl]
            [hop-cli.bootstrap.infrastructure :as infrastructure]
            [hop-cli.util.thread-transactions :as tht]))

(def ^:const cfn-templates-path
  (io/resource "infrastructure/cloudformation-templates"))

(def cfn-templates
  {:account {:master-template "account.yaml"
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :cloud-provider.aws.account/stack-name
             :input-parameter-mapping
             {:cloud-provider.aws.account.vpc/cidr :VpcCIDR
              :cloud-provider.aws.account/resource-name-prefix :ResourceNamePrefix}
             :output-parameter-mapping
             {:EbServiceRoleARN :cloud-provider.aws.account.iam/eb-service-role-arn
              :LocalDevUserARN :cloud-provider.aws.account.iam/local-dev-user-arn
              :RDSMonitoringRoleARN :cloud-provider.aws.account.iam/rds-monitoring-role-arm
              :PublicRouteTable1Id :cloud-provider.aws.account.vpc/public-route-table-id
              :VpcId :cloud-provider.aws.account.vpc/id}}

   :project {:master-template "project.yaml"
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :cloud-provider.aws.project/stack-name
             :input-parameter-mapping
             {:cloud-provider.aws.account.vpc/id :VpcId
              :cloud-provider.aws.account.vpc/public-route-table-id :PublicRouteTable1Id
              :cloud-provider.aws.project.vpc.subnet-1/cidr :Subnet1CIDR
              :cloud-provider.aws.project.vpc.subnet-2/cidr :Subnet2CIDR
              :cloud-provider.aws.project.elb/certificate-arn :ElbCertificateArn}
             :output-parameter-mapping
             {:EbApplicationName :cloud-provider.aws.project.eb/application-name
              :ElbSecurityGroupId :cloud-provider.aws.project.elb/security-group-id
              :LoadBalancerARN  :cloud-provider.aws.project.elb/arn
              :SubnetIds :cloud-provider.aws.project.vpc/subnet-ids}}

   :dev-env {:master-template "local-environment.yaml"
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :cloud-provider.aws.environment.dev/stack-name
             :environment "dev"
             :input-parameter-mapping
             {:cloud-provider.aws.account.iam/local-dev-user-arn :LocalDevUserARN}
             :output-parameter-mapping
             {:CognitoUserPoolId :cloud-provider.aws.environment.dev.cognito/user-pool-id
              :CognitoUserPoolURL :cloud-provider.aws.environment.dev.cognito/user-pool-url
              :CognitoSPAClientId :cloud-provider.aws.environment.dev.cognito/spa-client-id}}

   :test-env {:master-template "cloud-environment.yaml"
              :capability :CAPABILITY_NAMED_IAM
              :stack-name-kw :cloud-provider.aws.environment.test/stack-name
              :environment "test"
              :input-parameter-mapping
              {:project.profiles.persistence-sql.test.database/version :DatabaseEngineVersion
               :project.profiles.persistence-sql.test.database/port :DatabasePort
               :project.profiles.persistence-sql.test.database/name :DatabaseName
               :project.profiles.persistence-sql.test.admin-user/password :DatabasePassword
               :project.profiles.persistence-sql.test.admin-user/username :DatabaseUsername
               :cloud-provider.aws.environment.test/notifications-email :NotificationsEmail
               :cloud-provider.aws.account.iam/rds-monitoring-role-arm :RDSMonitoringRoleARN
               :cloud-provider.aws.account.vpc/id :VpcId
               :cloud-provider.aws.project.vpc/subnet-ids :SubnetIds
               :cloud-provider.aws.account.iam/eb-service-role-arn :EbServiceRoleARN
               :cloud-provider.aws.project.eb/application-name  :EbApplicationName
               :cloud-provider.aws.project.elb/arn :LoadBalancerARN
               :cloud-provider.aws.project.elb/security-group-id :ElbSecurityGroupId}
              :output-parameter-mapping
              {:CognitoUserPoolId :cloud-provider.aws.environment.test.cognito/user-pool-id
               :CognitoUserPoolURL :cloud-provider.aws.environment.test.cognito/user-pool-url
               :CognitoSPAClientId :cloud-provider.aws.environment.test.cognito/spa-client-id
               :RdsAddress :project.profiles.persistence-sql.test.database/host
               :EbEnvironmentName :cloud-provider.aws.environment.test.eb/environment-name
               :EbEnvironmentURL :cloud-provider.aws.environment.test.eb/environment-url}}

   :prod-env {:master-template "cloud-environment.yaml"
              :capability :CAPABILITY_NAMED_IAM
              :stack-name-kw :cloud-provider.aws.environment.prod/stack-name
              :environment "prod"
              :input-parameter-mapping
              {:project.profiles.persistence-sql.prod.database/version :DatabaseEngineVersion
               :project.profiles.persistence-sql.prod.database/port :DatabasePort
               :project.profiles.persistence-sql.prod.database/name :DatabaseName
               :project.profiles.persistence-sql.prod.admin-user/password :DatabasePassword
               :project.profiles.persistence-sql.prod.admin-user/username :DatabaseUsername
               :cloud-provider.aws.environment.prod/notifications-email :NotificationsEmail
               :cloud-provider.aws.account.iam/rds-monitoring-role-arm :RDSMonitoringRoleARN
               :cloud-provider.aws.account.vpc/id :VpcId
               :cloud-provider.aws.project.vpc/subnet-ids :SubnetIds
               :cloud-provider.aws.account.iam/eb-service-role-arn :EbServiceRoleARN
               :cloud-provider.aws.project.eb/application-name  :EbApplicationName
               :cloud-provider.aws.project.elb/arn :LoadBalancerARN
               :cloud-provider.aws.project.elb/security-group-id :ElbSecurityGroupId}
              :output-parameter-mapping
              {:CognitoUserPoolId :cloud-provider.aws.environment.prod.cognito/user-pool-id
               :CognitoUserPoolURL :cloud-provider.aws.environment.prod.cognito/user-pool-url
               :CognitoSPAClientId :cloud-provider.aws.environment.prod.cognito/spa-client-id
               :RdsAddress :project.profiles.persistence-sql.prod.database/host
               :EbEnvironmentName :cloud-provider.aws.environment.prod.eb/environment-name
               :EbEnvironmentURL :cloud-provider.aws.environment.prod.eb/environment-url}}})

(defn wait-for-stack-completion
  [stack-name]
  (loop []
    (let [result (aws.cloudformation/describe-stack {:stack-name stack-name})
          status (get-in result [:stack :status])]
      (cond
        (= status :CREATE_IN_PROGRESS)
        (do
          (println (format "%s stack creation in progress. Rechecking the status in 10 seconds..." stack-name))
          (Thread/sleep 10000)
          (recur))

        (= status :CREATE_COMPLETE)
        {:success? true
         :outputs (get-in result [:stack :outputs])}

        :else
        {:success? false
         :error-details result}))))

(defn- select-and-rename-keys
  [m mapping]
  (-> m
      (select-keys (keys mapping))
      (set/rename-keys mapping)))

(defn- provision-cfn-stack
  [settings {:keys [input-parameter-mapping output-parameter-mapping stack-name-kw] :as template-opts}]
  (let [stack-name (get settings stack-name-kw)
        project-name (:project/name settings)
        bucket-name (:cloud-provider.aws.cloudformation/template-bucket-name settings)
        parameters (select-and-rename-keys settings input-parameter-mapping)
        opts (assoc template-opts
                    :project-name project-name
                    :parameters parameters
                    :stack-name stack-name
                    :s3-bucket-name bucket-name)
        _log (println (format "Provisioning cloudformation %s stack..." stack-name))
        result (aws.cloudformation/create-stack opts)]
    (if (:success? result)
      (let [wait-result (wait-for-stack-completion stack-name)]
        (if-not (:success? wait-result)
          wait-result
          (let [outputs (:outputs wait-result)
                new-settings (select-and-rename-keys outputs output-parameter-mapping)
                updated-settings (merge settings new-settings)]
            {:success? true
             :settings updated-settings})))
      result)))

(defmethod infrastructure/provision-initial-infrastructure :aws
  [settings]
  (->
   [{:txn-fn
     (fn upload-cloudformation-templates
       [_]
       (let [bucket-name (:cloud-provider.aws.cloudformation/template-bucket-name settings)
             opts {:bucket-name bucket-name
                   :directory-path cfn-templates-path}
             _log (println (format "Uploading cloudformation templates to %s bucket..." bucket-name))
             result (aws.cloudformation/update-templates opts)]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-upload-cfn-templates
            :error-details result})))}
    {:txn-fn
     (fn provision-account
       [_]
       (let [{:keys [stack-name-kw output-parameter-mapping] :as template-opts} (:account cfn-templates)
             stack-name (get settings stack-name-kw)
             result (aws.cloudformation/describe-stack {:stack-name stack-name})]
         (if (and (:success? result) (:stack result))
           (let [outputs (get-in result [:stack :outputs])
                 new-settings (select-and-rename-keys outputs output-parameter-mapping)
                 updated-settings (merge settings new-settings)]
             (println "Skipping account stack creation because it already exists")
             {:success? true
              :settings updated-settings})
           (let [result (provision-cfn-stack settings template-opts)]
             (if (:success? result)
               {:success? true
                :settings (:settings result)}
               {:success? false
                :reason :could-not-provision-account-cfn
                :error-details result})))))}
    {:txn-fn
     (fn create-and-upload-self-signed-certificate
       [{:keys [settings]}]
       (if (:cloud-provider.aws.project.elb/certificate-arn settings)
         (do
           (println "Skipping self-signed certificate upload.")
           {:success? true
            :settings settings})
         (let [_log (println "Creating and uploading self-signed certificate...")
               result (aws.ssl/create-and-upload-self-signed-certificate {})]
           (if (:success? result)
             (let [certificate-arn (:certificate-arn result)
                   updated-settings (assoc settings
                                           :cloud-provider.aws.project.elb/certificate-arn certificate-arn)]
               {:success? true
                :settings updated-settings})
             {:success? false
              :reason :could-not-create-and-upload-self-signed-certificate
              :error-details result}))))}
    {:txn-fn
     (fn provision-project
       [{:keys [settings]}]
       (let [result (provision-cfn-stack settings (:project cfn-templates))]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :could-not-provision-project-cfn
            :error-details result})))}
    {:txn-fn
     (fn provision-dev-env
       [{:keys [settings]}]
       (let [result (provision-cfn-stack settings (:dev-env cfn-templates))]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :could-not-provision-dev-env
            :error-details result})))}
    {:txn-fn
     (fn provision-test-env
       [{:keys [settings]}]
       (let [result (provision-cfn-stack settings (:test-env cfn-templates))]
         (if (:success? result)
           {:success? true
            :settings (:settings result)}
           {:success? false
            :reason :could-not-provision-test-env
            :error-details result})))}]
   (tht/thread-transactions {})))

(defn provision-prod-infrastructure
  [settings]
  (provision-cfn-stack settings (:prod-env cfn-templates)))


(defmethod infrastructure/save-environment-variables :aws
  [settings]
  ;;TODO Fix this once we flatten the project settings coming from the profiles
  (let [environment-variables (get-in settings [:project :environment-variables])
        config {:project-name (:project/name settings)
                :environment "test"
                :kms-key-alias (:aws.environment.test.kms/key-alias settings)}
        ssm-env-vars (->> environment-variables
                          :test
                          walk/stringify-keys
                          (map zipmap (repeat [:name :value]))
                          (map #(update % :value str))
                          (filter (comp seq :value)))
        result (api.ssm/put-parameters config {:new? true} ssm-env-vars)]
    (if (:success? result)
      result
      {:success? false
       :error-details result})))
