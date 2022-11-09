(ns hop-cli.bootstrap.profile.authentication.cognito)

(defn cognito-adapter-config
  [_settings]
  {:dev.gethop.user-manager/cognito
   {:user-pool-id (tagged-literal 'duct/env ["COGNITO_USER_POOL_ID" 'Str])}})

(defn- build-env-variables
  [_settings _environment]
  {:COGNITO_USER_POOL_ID ""})

(defn profile
  [settings]
  {:dependencies '[[dev.gethop/session.re-frame.cognito "0.1.0-alpha"]
                   [dev.gethop/user-manager.cognito "0.1.0"]]
   :config-edn {:base (cognito-adapter-config settings)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}})
