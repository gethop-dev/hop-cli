(ns hop-cli.bootstrap.profile.registry.aws
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- build-dev-env-variables
  [settings]
  {:AWS_ROLE_ARN (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/arn)})

(defn- build-setup-aws-vault-local-dev-user-instructions
  [settings]
  (let [profile-prefix (bp.util/get-settings-value settings :project.profiles.aws.aws-vault/profile-prefix)
        local-dev-user-name (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/name)
        local-dev-user-profile (format "%s/%s" profile-prefix local-dev-user-name)
        access-key-id (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/access-key-id)
        secret-access-key (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/secret-access-key)]
    (if (and access-key-id secret-access-key)
      (with-out-str
        (println "Configure the local-development user that will be used across all the projects in this AWS account")
        (println (format "Run the following command: 'aws-vault add %s" local-dev-user-profile))
        (println (format "Access Key Id: %s" access-key-id))
        (println (format "Secret Access Key: %s" secret-access-key)))
      (with-out-str
        (println "Make sure you have the %s aws-vault profile configured" local-dev-user-profile)))))

(defn- build-setup-aws-vault-project-dev-user-instructions
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)
        profile-prefix (bp.util/get-settings-value settings :project.profiles.aws.aws-vault/profile-prefix)
        local-user-name (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/name)
        local-user-arn (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/arn)]
    (with-out-str
      (println (format "Configure the development role used in the %s project" project-name))
      (println "Add the following profile to your aws config file (usually in '~/.aws/config')")
      (println (format "[profile %s/%s-dev-env]" profile-prefix project-name))
      (println (format "source_profile = %s/%s" profile-prefix local-user-name))
      (println (format "role_arn = %s" local-user-arn))
      (println "You may want to share those details with other team members that will work on this project."))))

(defmethod registry/pre-render-hook :aws
  [_ settings]
  {:files [{:src "aws/.platform" :dst ".platform"}
           {:src "aws/start-dev.sh" :dst "start-dev.sh"}]
   :environment-variables {:dev (build-dev-env-variables settings)}})

(defmethod registry/post-render-hook :aws
  [_ settings]
  {:post-installation-messages [(build-setup-aws-vault-local-dev-user-instructions settings)
                                (build-setup-aws-vault-project-dev-user-instructions settings)]})
