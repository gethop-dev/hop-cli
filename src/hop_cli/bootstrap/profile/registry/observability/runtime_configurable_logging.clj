;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry.observability.runtime-configurable-logging
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- build-timbre-config
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)
        middleware-dynamic-log-level-kw
        (keyword (format "%s.service.timbre/middleware.dynamic-log-level" project-name))
        middleware-inject-git-tag-kw
        (keyword (format "%s.service.timbre/middleware.inject-git-tag" project-name))
        output-fn-with-git-tag-kw
        (keyword (format "%s.service.timbre/output-fn-with-git-tag" project-name))]
    {middleware-dynamic-log-level-kw
     {}
     middleware-inject-git-tag-kw
     {:git-tag (tagged-literal 'duct/env  ["GIT_TAG" 'Str :or "local"])}
     output-fn-with-git-tag-kw
     {}
     :duct.logger/timbre
     {:middleware [(tagged-literal 'ig/ref middleware-dynamic-log-level-kw)
                   (tagged-literal 'ig/ref middleware-inject-git-tag-kw)]
      :output-fn (tagged-literal 'ig/ref output-fn-with-git-tag-kw)}}))

(defn- build-api-config
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    {(keyword (format "%s.api/logging" project-name))
     (tagged-literal 'ig/ref (keyword project-name "common-config"))}))

(defn- build-api-routes
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    [(tagged-literal 'ig/ref (keyword (str project-name ".api/logging")))]))

(defmethod registry/pre-render-hook :observability-runtime-configurable-logging
  [_ settings]
  {:files [{:src "observability/runtime-configurable-logging"}]
   :config-edn {:api-routes (build-api-routes settings)
                :base (merge
                       (build-api-config settings)
                       (build-timbre-config settings))}})
