(ns hop-cli.bootstrap.profile.authentication.keycloak)

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

(defn keycloak-config
  []
  {:keycloak {:realm (tagged-literal 'duct/env ["KEYCLOAK_REALM" 'Str])
              :url (tagged-literal 'duct/env ["KEYCLOAK_URL" 'Str])
              :client-id (tagged-literal 'duct/env ["KEYCLOAK_CLIENT_ID" 'Str])}})

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
   ::init-keycloak
   (fn [{:keys [db]} _]
     {:init-and-try-to-authenticate {:oidc (get-in db [:config :keycloak])}}))")

(defn- build-env-variables
  [settings environment]
  (let [base-path (format "keycloak.environment.%s.realm" (name environment))
        keycloak-realm (get settings (keyword base-path "realm"))
        keycloak-url (get settings (keyword base-path "url"))
        keycloak-client-id (get settings (keyword base-path "client-id"))]
    {:KEYCLOAK_REALM keycloak-realm
     :KEYCLOAK_URL keycloak-url
     :KEYCLOAK_CLIENT_ID keycloak-client-id
     :OIDC_AUDIENCE keycloak-client-id
     :OIDC_ISSUER_URL (str keycloak-url "/" keycloak-realm "/" keycloak-realm)
     :OIDC_JWKS_URI (str keycloak-url "/" keycloak-realm "/protocol/openid-connect/certs")}))

(defn profile
  [settings]
  {:config-edn {:base (merge
                       (jwt-oidc-config settings)
                       (buddy-auth-config settings)
                       (user-api-config settings))
                :common-config (common-config)
                :config (keycloak-config)
                :routes (routes settings)}
   :load-frontend-app {:events ["[:dispatch [::init-keycloak]]"]
                       :code [load-frontend-app-code]}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files [{:src "authentication/common"}
           {:src "authentication/keycloak"}]})
