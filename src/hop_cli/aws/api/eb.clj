;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.eb
  (:require [com.grzm.awyeah.client.api :as aws]))

(defonce eb-client
  (aws/client {:api :elasticbeanstalk}))

(defn update-env-variable
  [{:keys [project-name environment]} {:keys [name value]}]
  (let [request {:EnvironmentName (str project-name "-" environment)
                 :OptionSettings [{:Namespace "aws:elasticbeanstalk:application:environment"
                                   :OptionName name
                                   :Value value}]}
        opts {:op :UpdateEnvironment
              :request request}
        result (aws/invoke eb-client opts)]
    (if (:category result)
      {:success? false
       :error-details result}
      result)))
