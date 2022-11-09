(ns hop-cli.bootstrap.profile.aws)

(defn- build-env-variables
  [_settings _environment]
  {:AWS_ROLE_ARN ""})

(defn profile
  [settings]
  {:files [{:src "aws/.platform" :dst ".platform"}]
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}})
