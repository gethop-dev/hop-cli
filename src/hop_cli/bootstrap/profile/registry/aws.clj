(ns hop-cli.bootstrap.profile.registry.aws)

(defn- build-dev-env-variables
  [settings]
  {:AWS_ROLE_ARN (:cloud-provider.aws.account.iam/eb-service-role-arn settings)})

(defn profile
  [settings]
  {:files [{:src "aws/.platform" :dst ".platform"}]
   :environment-variables {:dev (build-dev-env-variables settings)}})
