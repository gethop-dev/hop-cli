(ns hop-cli.bootstrap.profile.registry.authentication.keycloak
  (:require [hop-cli.bootstrap.util :as bootstrap.util]))

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
              :url (tagged-literal 'duct/env ["KEYCLOAK_FRONTEND_URL" 'Str])
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

(defn- build-external-env-variables
  [settings environment]
  (let [base-path (format "project.profiles.auth-keycloak.environment.%s.external.%s"
                          (name (bootstrap.util/get-env-type environment)) (name environment))]
    {:KEYCLOAK_REALM (get settings (keyword base-path "realm"))
     :KEYCLOAK_FRONTEND_URL (get settings (keyword base-path "frontend-url"))
     :KEYCLOAK_CLIENT_ID (get settings (keyword base-path "client-id"))}))

(defn- build-container-env-variables
  [settings environment]
  (let [keycloak-base-path (format "project.profiles.auth-keycloak.environment.%s.container.%s"
                                   (name (bootstrap.util/get-env-type environment))  (name environment))
        persistence-sql-base-path (format "project.profiles.persistence-sql.environment.%s" (name environment))
        keycloak-realm (get settings (keyword (str keycloak-base-path ".realm/name")))
        keycloak-frontend-url (get settings (keyword keycloak-base-path "frontend-url"))
        keycloak-client-id (get settings (keyword keycloak-base-path "client-id"))]
    ;; Application related environment variables
    {:KEYCLOAK_REALM keycloak-realm
     :KEYCLOAK_FRONTEND_URL keycloak-frontend-url
     :KEYCLOAK_CLIENT_ID keycloak-client-id
     :OIDC_AUDIENCE keycloak-client-id
     :OIDC_ISSUER_URL (str keycloak-frontend-url "/realms/" keycloak-realm)
     :OIDC_JWKS_URI (str keycloak-frontend-url "/realms/" keycloak-realm "/protocol/openid-connect/certs")

     ;; Keycloak service related environment variables
     :MEMORY_LIMIT_KEYCLOAK "256m"
     :KC_ADMIN (get settings (keyword (str keycloak-base-path ".admin/username")))
     :KC_ADMIN_PASSWORD (get settings (keyword (str keycloak-base-path ".admin/password")))
     :KC_DB "postgres"
     :KC_DB_URL (format "jdbc:postgresql://%s:%s/%s"
                              (get settings (keyword (str persistence-sql-base-path ".database/host")))
                              (get settings (keyword (str persistence-sql-base-path ".database/port")))
                              (get settings (keyword (str persistence-sql-base-path ".database/name"))))
     :KC_DB_SCHEMA (get settings (keyword (str keycloak-base-path ".database/schema")))
     :KC_DB_USERNAME (get settings (keyword (str keycloak-base-path ".database/username")))
     :KC_DB_PASSWORD (get settings (keyword (str keycloak-base-path ".database/password")))
     :KC_PROXY "edge"}))

(defn- build-env-variables
  [settings environment]
  (if (= :external (get settings (keyword (format "project.profiles.auth-keycloak.environment.%s/value"
                                                  (name (bootstrap.util/get-env-type environment))))))
    (build-external-env-variables settings environment)
    (build-container-env-variables settings environment)))

(defn- docker-compose-files
  [settings]
  (let [common ["docker-compose.keycloak.yml"]
        common-dev-ci ["docker-compose.keycloak.common-dev-ci.yml"]
        ci ["docker-compose.keycloak.ci.yml"]
        aws ["docker-compose.keycloak.aws.yml"]]
    (cond->  {:to-develop [] :ci [] :to-deploy []}
      (= :container (:project.profiles.auth-keycloak.environment.to-develop/value settings))
      (assoc :to-develop (concat common common-dev-ci)
             :ci (concat common common-dev-ci ci))

      (= :container (:project.profiles.auth-keycloak.environment.to-deploy/value settings))
      (assoc :to-deploy common)

      (= :aws (:cloud-provider/value settings))
      (update :to-deploy concat aws))))

(defn- build-docker-files-to-copy
  [settings]
  (let [compose-files
        (->> (docker-compose-files settings)
             (vals)
             (apply concat)
             (map (fn [file] {:src (str "/authentication/keycloak/" file)})))]
    (if (seq compose-files)
      (conj compose-files {:src "/authentication/keycloak/keycloak" :dst "keycloak"})
      [])))

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
   :files (concat [{:src "authentication/common"}
                   {:src "authentication/keycloak/app" :dst "app"}]
                  (build-docker-files-to-copy settings))
   :docker-compose (docker-compose-files settings)})
