;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.resourcegroupstagging
  (:require [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn get-resource-arns
  [{:keys [tags resource-types] :as opts}]
  (let [client (aws.client/gen-client :resourcegroupstaggingapi opts)
        request {:ResourceTypeFilters resource-types
                 :TagFilters (map (fn [[k v]] {:Key k :Values [v]}) tags)}
        args {:op :GetResources
              :request request}
        result (aws/invoke client args)]
    (if (:ResourceTagMappingList result)
      {:success? true
       :resource-arns (map :ResourceARN (:ResourceTagMappingList result))}
      {:success? false
       :error-details result})))
