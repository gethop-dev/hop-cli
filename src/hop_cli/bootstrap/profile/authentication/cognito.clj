(ns hop-cli.bootstrap.profile.authentication.cognito)

(defn- jwt-oidc-config
  [_settings]
  {:dev.gethop.buddy-auth/jwt-oidc
   {:claims
    {:iss (tagged-literal 'duct/env ["OIDC_ISSUER_URL" 'Str])
     :aud (tagged-literal 'duct/env ["OIDC_AUDIENCE" 'Str])}
    :jwks-uri (tagged-literal 'duct/env ["OIDC_JWKS_URI" 'Str])
    :logger (tagged-literal 'ig/ref :duct/logger)}})

(defn- buddy-auth-config
  [_settings]
  {:duct.middleware.buddy/authentication
   {:backend :token
    :token-name "Bearer"
    :authfn (tagged-literal 'ig/ref :dev.gethop.buddy-auth/jwt-oidc)}})

(defn cognito-adapter-config
  [_settings]
  {:dev.gethop.user-manager/cognito
   {:user-pool-id (tagged-literal 'duct/env ["COGNITO_USER_POOL_ID" 'Str])}})

(defn cognito-config
  []
  {:cognito {:iss (tagged-literal 'duct/env ["OIDC_ISSUER_URL"])
             :client-id (tagged-literal 'duct/env ["OIDC_AUDIENCE"])}})

(defn user-api-config
  [settings]
  {(keyword (str (:project/name settings) ".api/user"))
   (tagged-literal 'ig/ref (keyword (:project/name settings) "common-config"))})

(defn- common-config
  []
  {:auth-middleware (tagged-literal 'ig/ref :duct.middleware.buddy/authentication)})

(defn- routes
  [settings]
  [(tagged-literal 'ig/ref (keyword (str (:project/name settings) ".api/user")))])

(def ^:private load-frontend-app-code
  "(rf/reg-event-fx
   ::init-cognito
   (fn [{:keys [db]} _]
     {:dispatch [::session/set-config {:oidc (get-in db [:config :cognito])}]}))")

(defn- build-env-variables
  [settings environment]
  (let [base-path (format "cloud-provider.aws.environment.%s.cognito" (name environment))
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
   :config-edn {:base (merge
                       (cognito-adapter-config settings)
                       (jwt-oidc-config settings)
                       (buddy-auth-config settings)
                       (user-api-config settings))
                :common-config (common-config)
                :config (cognito-config)
                :routes (routes settings)}
   :load-frontend-app {:requires '[[dev.gethop.session.re-frame.cognito :as session]]
                       :events ["[:dispatch [::init-cognito]]"]
                       :code [load-frontend-app-code]}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files [{:src "authentication/common"}
           {:src "authentication/cognito"}]})
