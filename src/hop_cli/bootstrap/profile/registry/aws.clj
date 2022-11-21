(ns hop-cli.bootstrap.profile.registry.aws
  (:require [hop-cli.bootstrap.util :as bp.util]))

(defn- build-dev-env-variables
  [settings]
  {:AWS_ROLE_ARN (bp.util/get-settings-value settings :cloud-provider.aws.account.iam/eb-service-role-arn)})

(defn profile
  [settings]
  {:files [{:src "aws/.platform" :dst ".platform"}]
   :environment-variables {:dev (build-dev-env-variables settings)}})
