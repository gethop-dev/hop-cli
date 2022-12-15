(ns hop-cli.bootstrap.infrastructure.aws
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [hop-cli.aws.api.ssm :as api.ssm]
            [hop-cli.aws.api.sts :as api.sts]
            [hop-cli.aws.cloudformation :as aws.cloudformation]
            [hop-cli.aws.iam :as aws.iam]
            [hop-cli.aws.ssl :as aws.ssl]
            [hop-cli.bootstrap.infrastructure :as infrastructure]
            [hop-cli.bootstrap.util :as bp.util]
            [hop-cli.util.thread-transactions :as tht]
            [meta-merge.core :refer [meta-merge]]))

(def ^:const cfn-templates-path
  (io/resource "infrastructure/cloudformation-templates"))

(def cfn-templates
  {:account {:master-template "account.yaml"
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :cloud-provider.aws.account/stack-name
             :iam-users
             [:cloud-provider.aws.account.iam.local-dev-user/name
              :cloud-provider.aws.account.iam.ci-user/name]
             :input-parameter-mapping
             {:cloud-provider.aws.account.vpc/cidr :VpcCIDR
              :cloud-provider.aws.account/resource-name-prefix :ResourceNamePrefix}
             :output-parameter-mapping
             {:EbServiceRoleARN :cloud-provider.aws.account.iam/eb-service-role-arn
              :LocalDevUserARN :cloud-provider.aws.account.iam.local-dev-user/arn
              :LocalDevUserName :cloud-provider.aws.account.iam.local-dev-user/name
              :CiUserARN :cloud-provider.aws.account.iam.ci-user/arn
              :CiUserName :cloud-provider.aws.account.iam.ci-user/name
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
              :SubnetIds :cloud-provider.aws.project.vpc/subnet-ids
              :EcrAppRepositoryURL :cloud-provider.aws.project.ecr/app-repository-url}}

   :dev-env {:master-template "local-environment.yaml"
             :capability :CAPABILITY_NAMED_IAM
             :stack-name-kw :cloud-provider.aws.environment.dev/stack-name
             :environment "dev"
             :input-parameter-mapping
             {:cloud-provider.aws.account.iam.local-dev-user/arn :LocalDevUserARN
              :cloud-provider.aws.environment.dev.optional-services.cognito/enabled :IncludeCognito
              :cloud-provider.aws.environment.dev.optional-services.s3/enabled :IncludeS3
              :cloud-provider.aws.environment.dev.optional-services.cloudwatch/enabled :IncludeCloudwatch
              :cloud-provider.aws.environment.dev.optional-services.cloudwatch/log-group-name :CloudwatchLogGroupName
              :cloud-provider.aws.environment.dev.optional-services.cloudwatch/retention-days :CloudwatchLogRetentionDays}
             :output-parameter-mapping
             {:S3BucketName :cloud-provider.aws.environment.dev.optional-services.s3/bucket-name
              :CognitoUserPoolId :cloud-provider.aws.environment.dev.optional-services.cognito.user-pool/id
              :CognitoUserPoolURL :cloud-provider.aws.environment.dev.optional-services.cognito.user-pool/url
              :CognitoSPAClientId :cloud-provider.aws.environment.dev.optional-services.cognito.user-pool.app-client/id
              :CloudwatchLogGroupName :cloud-provider.aws.environment.dev.optional-services.cloudwatch/log-group-name
              :DevRoleArn :cloud-provider.aws.environment.dev.iam.role/arn}}

   :test-env {:master-template "cloud-environment.yaml"
              :capability :CAPABILITY_NAMED_IAM
              :stack-name-kw :cloud-provider.aws.environment.test/stack-name
              :environment "test"
              :input-parameter-mapping
              {:cloud-provider.aws.environment.test.optional-services.rds/version :DatabaseEngineVersion
               :cloud-provider.aws.environment.test.optional-services.rds/port :DatabasePort
               :cloud-provider.aws.environment.test.optional-services.rds/name :DatabaseName
               :cloud-provider.aws.environment.test.optional-services.rds.admin-user/password :DatabasePassword
               :cloud-provider.aws.environment.test.optional-services.rds.admin-user/username :DatabaseUsername
               :cloud-provider.aws.environment.test/notifications-email :NotificationsEmail
               :cloud-provider.aws.account.iam/rds-monitoring-role-arm :RDSMonitoringRoleARN
               :cloud-provider.aws.account.vpc/id :VpcId
               :cloud-provider.aws.project.vpc/subnet-ids :SubnetIds
               :cloud-provider.aws.account.iam/eb-service-role-arn :EbServiceRoleARN
               :cloud-provider.aws.project.eb/application-name  :EbApplicationName
               :cloud-provider.aws.project.elb/arn :LoadBalancerARN
               :cloud-provider.aws.project.elb/security-group-id :ElbSecurityGroupId
               :cloud-provider.aws.environment.test.optional-services.cognito/enabled :IncludeCognito
               :cloud-provider.aws.environment.test.optional-services.s3/enabled :IncludeS3
               :cloud-provider.aws.environment.test.optional-services.rds/enabled :IncludeRds
               :cloud-provider.aws.environment.test.optional-services.cloudwatch/enabled :IncludeCloudwatch
               :cloud-provider.aws.environment.test.eb/ec2-instance-type :EbInstanceType
               :cloud-provider.aws.environment.test.optional-services.cloudwatch/log-group-name :CloudwatchLogGroupName
               :cloud-provider.aws.environment.test.optional-services.cloudwatch/retention-days :CloudwatchLogRetentionDays}
              :output-parameter-mapping
              {:CognitoUserPoolId :cloud-provider.aws.environment.test.optional-services.cognito.user-pool/id
               :CognitoUserPoolURL :cloud-provider.aws.environment.test.optional-services.cognito.user-pool/url
               :CognitoSPAClientId :cloud-provider.aws.environment.test.optional-services.cognito.user-pool.app-client/id
               :RdsAddress :cloud-provider.aws.environment.test.optional-services.rds/host
               :EbEnvironmentName :cloud-provider.aws.environment.test.eb/environment-name
               :EbEnvironmentURL :cloud-provider.aws.environment.test.eb/environment-url
               :S3BucketName :cloud-provider.aws.environment.test.optional-services.s3/bucket-name
               :CloudwatchLogGroupName :cloud-provider.aws.environment.test.optional-services.cloudwatch/log-group-name}}

   :prod-env {:master-template "cloud-environment.yaml"
              :capability :CAPABILITY_NAMED_IAM
              :stack-name-kw :cloud-provider.aws.environment.prod/stack-name
              :environment "prod"
              :input-parameter-mapping
              {:cloud-provider.aws.environment.prod.optional-services.rds/version :DatabaseEngineVersion
               :cloud-provider.aws.environment.prod.optional-services.rds/port :DatabasePort
               :cloud-provider.aws.environment.prod.optional-services.rds/name :DatabaseName
               :cloud-provider.aws.environment.prod.optional-services.rds.admin-user/password :DatabasePassword
               :cloud-provider.aws.environment.prod.optional-services.rds.admin-user/username :DatabaseUsername
               :cloud-provider.aws.environment.prod/notifications-email :NotificationsEmail
               :cloud-provider.aws.account.iam/rds-monitoring-role-arm :RDSMonitoringRoleARN
               :cloud-provider.aws.account.vpc/id :VpcId
               :cloud-provider.aws.project.vpc/subnet-ids :SubnetIds
               :cloud-provider.aws.account.iam/eb-service-role-arn :EbServiceRoleARN
               :cloud-provider.aws.project.eb/application-name  :EbApplicationName
               :cloud-provider.aws.project.elb/arn :LoadBalancerARN
               :cloud-provider.aws.project.elb/security-group-id :ElbSecurityGroupId
               :cloud-provider.aws.environment.prod.optional-services.cognito/enabled :IncludeCognito
               :cloud-provider.aws.environment.prod.optional-services.s3/enabled :IncludeS3
               :cloud-provider.aws.environment.prod.optional-services.rds/enabled :IncludeRds
               :cloud-provider.aws.environment.prod.optional-services.cloudwatch/enabled :IncludeCloudwatch
               :cloud-provider.aws.environment.prod.eb/ec2-instance-type :EbInstanceType
               :cloud-provider.aws.environment.prod.optional-services.cloudwatch/log-group-name :CloudwatchLogGroupName
               :cloud-provider.aws.environment.prod.optional-services.cloudwatch/retention-days :CloudwatchLogRetentionDays}
              :output-parameter-mapping
              {:CognitoUserPoolId :cloud-provider.aws.environment.prod.optional-services.cognito.user-pool/id
               :CognitoUserPoolURL :cloud-provider.aws.environment.prod.optional-services.cognito.user-pool/url
               :CognitoSPAClientId :cloud-provider.aws.environment.prod.optional-services.cognito.user-pool.app-client/id
               :RdsAddress :cloud-provider.aws.environment.prod.optional-services.rds/host
               :EbEnvironmentName :cloud-provider.aws.environment.prod.eb/environment-name
               :EbEnvironmentURL :cloud-provider.aws.environment.prod.eb/environment-url
               :S3BucketName :cloud-provider.aws.environment.prod.optional-services.s3/bucket-name
               :CloudwatchLogGroupName :cloud-provider.aws.environment.prod.optional-services.cloudwatch/log-group-name}}})

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

(defn select-and-rename-keys
  [m mapping]
  (reduce-kv
   (fn [r k1 k2]
     (let [p1 (bp.util/settings-kw->settings-path k1)
           p2 (bp.util/settings-kw->settings-path k2)
           v (get-in m p1)]
       (cond-> r
         (some? v) (assoc-in p2 v))))
   {}
   mapping))

(defn- provision-iam-user-access-key
  [settings name-kw]
  (let [user-name (bp.util/get-settings-value settings name-kw)
        result (aws.iam/create-access-key {:username user-name})]
    (if-not (:success? result)
      {:success? false
       :reason :could-not-create-iam-user-credentials
       :error-details {:name-kw name-kw :result result}}
      (let [path (butlast (bp.util/settings-kw->settings-path name-kw))]
        {:success? true
         :settings (-> settings
                       (bp.util/merge-settings-value path (:access-key result)))}))))

(defn- provision-iam-user-access-keys
  [settings name-kws]
  (reduce
   (fn [{:keys [settings]} name-kw]
     (let [result (provision-iam-user-access-key settings name-kw)]
       (if (:success? result)
         result
         (reduced {:success? false :error-details result}))))
   {:success? true :settings settings}
   name-kws))

(defn- provision-cfn-stack
  [settings {:keys [input-parameter-mapping output-parameter-mapping
                    stack-name-kw iam-users] :as template-opts}]
  (let [stack-name (bp.util/get-settings-value settings stack-name-kw)
        project-name (bp.util/get-settings-value settings :project/name)
        bucket-name (bp.util/get-settings-value settings :cloud-provider.aws.cloudformation/template-bucket-name)
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
                updated-settings (meta-merge settings new-settings)]
            (if (empty? iam-users)
              {:success? true :settings updated-settings}
              (let [result (provision-iam-user-access-keys updated-settings iam-users)]
                (if-not (:success? result)
                  {:success? false
                   :reason :could-not-provision-iam-user-access-keys
                   :error-details result}
                  {:success? true
                   :settings (:settings result)}))))))
      result)))

(defn- get-or-provision-cfn-stack
  [settings {:keys [stack-name-kw output-parameter-mapping] :as template-opts}]
  (let [stack-name (bp.util/get-settings-value settings stack-name-kw)
        result (aws.cloudformation/describe-stack {:stack-name stack-name})]
    (if (and (:success? result) (:stack result))
      (let [outputs (get-in result [:stack :outputs])
            new-settings (select-and-rename-keys outputs output-parameter-mapping)
            updated-settings (meta-merge settings new-settings)]
        (println "Skipping account stack creation because it already exists")
        {:success? true
         :settings updated-settings})
      (provision-cfn-stack settings template-opts))))

(defmethod infrastructure/provision-initial-infrastructure :aws
  [settings]
  (let [environments (set (bp.util/get-settings-value settings :project/environments))]
    (->
     [{:txn-fn
       (fn get-aws-account-identity
         [_]
         (let [{:keys [success? caller-identity] :as result}
               (api.sts/get-caller-identity)]
           (if success?
             {:success? true
              :settings (bp.util/assoc-in-settings-value settings
                                                         :cloud-provider.aws.account/id
                                                         (:Account caller-identity))}
             {:success? false
              :reason :failed-to-get-aws-account-id
              :error-details result})))}
      {:txn-fn
       (fn upload-cloudformation-templates
         [{:keys [settings]}]
         (let [account-id (bp.util/get-settings-value settings :cloud-provider.aws.account/id)
               bucket-name (str "cloudformation-templates-" account-id)
               opts {:bucket-name bucket-name
                     :directory-path cfn-templates-path}
               _log (println (format "Uploading cloudformation templates to %s bucket..." bucket-name))
               result (aws.cloudformation/update-templates opts)]
           (if (:success? result)
             {:success? true
              :settings (bp.util/assoc-in-settings-value settings
                                                         :cloud-provider.aws.cloudformation/template-bucket-name
                                                         bucket-name)}
             {:success? false
              :reason :could-not-upload-cfn-templates
              :error-details result})))}
      {:txn-fn
       (fn provision-account
         [{:keys [settings]}]
         (let [result (get-or-provision-cfn-stack settings (:account cfn-templates))]
           (if-not (:success? result)
             {:success? false
              :reason :could-not-provision-account-cfn}
             result)))}
      {:txn-fn
       (fn create-and-upload-self-signed-certificate
         [{:keys [settings]}]
         (if (bp.util/get-settings-value settings :cloud-provider.aws.project.elb/certificate-arn)
           (do
             (println "Skipping self-signed certificate upload.")
             {:success? true
              :settings settings})
           (let [_log (println "Creating and uploading self-signed certificate...")
                 result (aws.ssl/create-and-upload-self-signed-certificate {})]
             (if (:success? result)
               (let [certificate-arn (:certificate-arn result)
                     updated-settings
                     (assoc-in settings [:cloud-provider :aws :project :elb :certificate-arn] certificate-arn)]
                 {:success? true
                  :settings updated-settings})
               {:success? false
                :reason :could-not-create-and-upload-self-signed-certificate
                :error-details result}))))}
      {:txn-fn
       (fn provision-project
         [{:keys [settings]}]
         (let [result (get-or-provision-cfn-stack settings (:project cfn-templates))]
           (if (:success? result)
             {:success? true
              :settings (:settings result)}
             {:success? false
              :reason :could-not-provision-project-cfn
              :error-details result})))}
      {:txn-fn
       (fn provision-dev-env
         [{:keys [settings] :as prv-result}]
         (if-not (get environments :dev)
           prv-result
           (let [result (provision-cfn-stack settings (:dev-env cfn-templates))]
             (if (:success? result)
               {:success? true
                :settings (:settings result)}
               {:success? false
                :reason :could-not-provision-dev-env
                :error-details result}))))}
      {:txn-fn
       (fn provision-test-env
         [{:keys [settings] :as prv-result}]
         (if-not (get environments :test)
           prv-result
           (let [result (provision-cfn-stack settings (:test-env cfn-templates))]
             (if (:success? result)
               {:success? true
                :settings (:settings result)}
               {:success? false
                :reason :could-not-provision-test-env
                :error-details result}))))}
      {:txn-fn
       (fn provision-prod-env
         [{:keys [settings] :as prv-result}]
         (if-not (get environments :prod)
           prv-result
           (let [result (provision-cfn-stack settings (:prod-env cfn-templates))]
             (if (:success? result)
               {:success? true
                :settings (:settings result)}
               {:success? false
                :reason :could-not-provision-prod-env
                :error-details result}))))}]
     (tht/thread-transactions {}))))

(defn- save-environment-variables
  [settings environment]
  (let [config {:environment (name environment)
                :project-name
                (bp.util/get-settings-value settings :project/name)
                :kms-key-alias
                (bp.util/get-settings-value settings [:aws :environment environment :kms :key-alias])}
        ssm-env-vars (->> (bp.util/get-settings-value settings :project/environment-variables)
                          environment
                          walk/stringify-keys
                          (map zipmap (repeat [:name :value]))
                          (map #(update % :value str))
                          (filter (comp seq :value)))
        result (api.ssm/put-parameters config {:new? true} ssm-env-vars)]
    (if (:success? result)
      result
      {:success? false
       :error-details result})))

(defmethod infrastructure/save-environment-variables :aws
  [settings]
  (let [environments (->> (bp.util/get-settings-value settings :project/environments)
                          (remove #{:dev})
                          (set))
        results (map (partial save-environment-variables settings) environments)]
    (if (every? :success? results)
      {:success? true}
      {:success? false
       :error-details (zipmap environments results)})))
