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
