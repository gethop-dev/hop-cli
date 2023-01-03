;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

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
        (println "Run the following command:")
        (println (format "    aws-vault add --add-config %s" local-dev-user-profile))
        (println "And use these values when asked:")
        (println (format "    Access Key Id: %s" access-key-id))
        (println (format "    Secret Access Key: %s" secret-access-key)))
      (with-out-str
        (println "The account AWS stack was already created, so no new local development AWS user was created.")
        (println (format "Make sure you have the '%s' aws-vault profile configured" local-dev-user-profile))))))

(defn- build-setup-aws-vault-project-dev-role-instructions
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)
        profile-prefix (bp.util/get-settings-value settings :project.profiles.aws.aws-vault/profile-prefix)
        local-user-name (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/name)
        local-role-arn (bp.util/get-settings-value settings :project.profiles.aws.credentials.local-dev-user/role-arn)
        region (bp.util/get-settings-value settings :project.profiles.aws/region)]
    (with-out-str
      (println (format "Configure the development role used in the %s project" project-name))
      (println "Add the following profile to your AWS config file (usually in '~/.aws/config')")
      (println (format "    [profile %s/%s-dev-env]" profile-prefix project-name))
      (println (format "    source_profile = %s/%s" profile-prefix local-user-name))
      (println (format "    role_arn = %s" local-role-arn))
      (println (format "    region = %s" region))
      (println "You may want to share those details with other team members that will work on this project."))))

(defn- build-print-ci-credentials-message
  [settings]
  (let [access-key-id (bp.util/get-settings-value settings :project.profiles.aws.credentials.ci-user/access-key-id)
        secret-access-key (bp.util/get-settings-value settings :project.profiles.aws.credentials.ci-user/secret-access-key)
        region (bp.util/get-settings-value settings :project.profiles.aws/region)]
    (if (and access-key-id secret-access-key)
      (with-out-str
        (println (format "A new AWS user was created for CI/CD purposes. You will need to configure the credentials in your CI/CD provider."))
        (println (format "    AWS_ACCESS_KEY_ID: %s" access-key-id))
        (println (format "    AWS_SECRET_ACCESS_KEY: %s" secret-access-key))
        (println (format "    AWS_DEFAULT_REGION: %s" region)))
      (with-out-str
        (println "The account AWS stack was already created, so no new CI/CD AWS user was created.")
        (println "You can reuse the credentials created in previous projects, or create new ones.")))))

(defmethod registry/pre-render-hook :aws
  [_ settings]
  {:files [{:src "aws/.platform" :dst ".platform"}
           {:src "aws/start-dev.sh" :dst "start-dev.sh"}]
   :environment-variables {:dev (build-dev-env-variables settings)}
   :deploy-files [".platform"]})

(defmethod registry/post-render-hook :aws
  [_ settings]
  {:post-installation-messages {:dev [(build-setup-aws-vault-local-dev-user-instructions settings)
                                      (build-setup-aws-vault-project-dev-role-instructions settings)
                                      (build-print-ci-credentials-message settings)]}})
