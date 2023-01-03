;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.ec2
  (:require [com.grzm.awyeah.client.api :as aws]
            [hop-cli.aws.api.client :as aws.client]))

(defn- filters->api-filters
  [filters]
  (map (fn [[k v]] {:Name (name k) :Values [v]}) filters))

(defn- api-instance->instance
  [{:keys [InstanceId]}]
  {:id InstanceId})

(defn- api-reservations->instances
  [reservations]
  (->> reservations
       (mapcat :Instances)
       (map api-instance->instance)))

(defn describe-instances
  [{:keys [filters max-results] :as opts}]
  (let [client (aws.client/gen-client :ec2 opts)
        request {:Filters (filters->api-filters filters)
                 :MaxResults max-results}
        args {:op :DescribeInstances
              :request request}
        result (aws/invoke client args)]
    (if (:Reservations result)
      {:success? true
       :instances (api-reservations->instances (:Reservations result))}
      {:success? false
       :error-details result})))
