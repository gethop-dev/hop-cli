(ns hop-cli.bootstrap.profile.authentication.cognito)

(defn cognito-adapter-config
  [_settings]
  {:dev.gethop.user-manager/cognito
   {:user-pool-id (tagged-literal 'duct/env ["COGNITO_USER_POOL_ID" 'Str])}})

(defn- build-env-variables
  [settings environment]
  (let [base-path (format "aws.environment.%s.cognito" (name environment))
        pool-id (get settings (keyword base-path "user-pool-id"))
        pool-url (get settings (keyword base-path "user-pool-url"))
        client-id (get settings (keyword base-path "spa-client-id"))]
    {:COGNITO_USER_POOL_ID pool-id
     :OIDC_AUDIENCE client-id
     :OIDC_ISSUER_URL pool-url
     :OIDC_JWKS_URI (str pool-url "/.well-known/jwks.json")}))

(defn profile
  [settings]
  {:dependencies '[[dev.gethop/session.re-frame.cognito "0.1.0-alpha"]
                   [dev.gethop/user-manager.cognito "0.1.0"]]
   :config-edn {:base (cognito-adapter-config settings)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}})
