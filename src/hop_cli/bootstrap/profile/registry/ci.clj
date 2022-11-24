(ns hop-cli.bootstrap.profile.registry.ci
  (:require [hop-cli.bootstrap.util :as bp.util]))

(defn profile
  [settings]
  {:files (cond-> [{:src "ci/common"}]
            (bp.util/get-settings-value settings :project.profiles.ci.provider.bitbucket-pipelines/enabled)
            (conj {:src "ci/bitbucket-pipelines"})
            (bp.util/get-settings-value settings :project.profiles.ci.provider.github-actions/enabled)
            (conj {:src "ci/github-actions"})
            (bp.util/get-settings-value settings :project.profiles.ci.continuous-deployment.aws/enabled)
            (conj {:src "ci/aws/ci" :dst "ci/aws"}))})
