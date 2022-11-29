(ns hop-cli.bootstrap.profile.registry.aws
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- build-dev-env-variables
  [settings]
  {:AWS_ROLE_ARN (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/arn)})

(defn- build-setup-aws-vault-instructions
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)
        profile-prefix (bp.util/get-settings-value settings :project.profiles.aws.aws-vault/profile-prefix)
        local-user-name (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/name)
        local-user-arn (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/arn)]
    (with-out-str
      (println "AWS local setup")
      (println "Add the following profile to your aws config file (usually in '~/.aws/config')")
      (println (format "[profile %s/%s-dev-env]" profile-prefix project-name))
      (println (format "source_profile = %s/%s" profile-prefix local-user-name))
      (println (format "role_arn = %s" local-user-arn)))))

(defmethod registry/pre-render-hook :aws
  [_ settings]
  {:files [{:src "aws/.platform" :dst ".platform"}]
   :environment-variables {:dev (build-dev-env-variables settings)}})

(defmethod registry/post-render-hook :aws
  [_ settings]
  {:post-installation-messages [(build-setup-aws-vault-instructions settings)]})
