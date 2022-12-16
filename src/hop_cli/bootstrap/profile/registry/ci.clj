(ns hop-cli.bootstrap.profile.registry.ci
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defmethod registry/pre-render-hook :ci
  [_ settings]
  {:files (cond-> [{:src "ci/common"}]
            (bp.util/get-settings-value settings :project.profiles.ci.provider.bitbucket-pipelines/enabled)
            (conj {:src "ci/bitbucket-pipelines"})
            (bp.util/get-settings-value settings :project.profiles.ci.provider.github-actions/enabled)
            (conj {:src "ci/github-actions"})
            (bp.util/get-settings-value settings :project.profiles.ci.continuous-deployment.provider.aws/enabled)
            (conj {:src "ci/aws/ci" :dst "ci/aws"}))})
