;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry.object-storage.s3
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- object-storage-adapter-config
  [_settings]
  {:dev.gethop.object-storage/s3 {:bucket-name (tagged-literal 'duct/env ["S3_BUCKET_NAME" 'Str])
                                  :presigned-url-lifespan 30}})

(defn- build-env-variables
  [settings environment]
  {:S3_BUCKET_NAME
   (bp.util/get-settings-value settings [:project :profiles :object-storage-s3 :environment environment :bucket :? :name])})

(defmethod registry/pre-render-hook :object-storage-s3
  [_ settings]
  {:dependencies '[[dev.gethop/object-storage.s3 "0.6.10"]]
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :config-edn {:base (object-storage-adapter-config settings)}})
