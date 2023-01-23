;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

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
            (conj {:src "ci/aws/ci" :dst "ci/aws"})
            (bp.util/get-settings-value settings :project.profiles.ci.continuous-deployment.provider.on-premises/enabled)
            (conj {:src "ci/on-premises/ci" :dst "ci/on-premises"}))})
