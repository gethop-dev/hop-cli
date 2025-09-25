;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry.authentication.keycloak
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]
            [meta-merge.core :refer [meta-merge]]))

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

(defn- keycloak-config
  []
  {:keycloak {:realm (tagged-literal 'duct/env ["KEYCLOAK_REALM" 'Str])
              :url (tagged-literal 'duct/env ["KEYCLOAK_URI" 'Str])
              :client-id (tagged-literal 'duct/env ["KEYCLOAK_APP_CLIENT_ID" 'Str])}})

(defn- user-api-config
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    {(keyword (str project-name ".api/user"))
     (tagged-literal 'ig/ref (keyword project-name "common-config"))}))

(defn- common-config
  []
  {:auth-middleware (tagged-literal 'ig/ref :duct.middleware.buddy/authentication)})

(defn- api-routes
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    [(tagged-literal 'ig/ref (keyword (str project-name ".api/user")))]))

(def ^:private load-frontend-app-client-code
  "(rf/reg-event-fx
    ::on-auth-failure
    (fn [_ _]
      {:fx [[:dispatch [::routes/init-routes]]]}))

   (rf/reg-event-fx
    ::on-auth-success
    (fn [_ _]
      {:fx [[:dispatch [::routes/init-routes]]]}))

   (rf/reg-event-fx
    ::init-keycloak
    (fn [{:keys [db]} _]
      {::session/init-and-try-to-authenticate
       {:config {:oidc (get-in db [:config :keycloak])}
        :on-auth-success-evt [::on-auth-success]
        :on-auth-failure-evt [::on-auth-failure]}}))")

(def ^:private navigated-event-code
  "(rf/reg-event-fx
    ::navigated
    [(rf/inject-cofx :session)]
    (fn [{{:keys [jwt-token]} :session} [_ new-match]]
      (cond
        (and (= :required (get-in new-match [:data :authentication])) jwt-token)
        {:dispatch [::apply-nav-route new-match]}

        (= :optional (get-in new-match [:data :authentication]))
        {:dispatch [::apply-nav-route new-match]}

        (and (= :not-allowed (get-in new-match [:data :authentication])) jwt-token)
        {:fx [[:dispatch [::nav/push-state (:name view.forbidden/route-config)]]]}

        (and (= :not-allowed (get-in new-match [:data :authentication])) (not jwt-token))
        {:dispatch [::apply-nav-route new-match]}

        :else
        {:fx [[:dispatch [::session/user-login]]]})))")

(defn- build-external-env-variables
  [settings env-path]
  (let [keycloak-realm (bp.util/get-settings-value settings (conj env-path :realm :name))
        keycloak-uri (bp.util/get-settings-value settings (conj env-path :uri))
        keycloak-app-client-id (bp.util/get-settings-value settings (conj env-path :app-client :id))]
    {:KEYCLOAK_REALM keycloak-realm
     :KEYCLOAK_URI keycloak-uri
     :KEYCLOAK_APP_CLIENT_ID keycloak-app-client-id}))

(defn- build-container-env-variables
  [settings environment deployment-type env-path]
  (let [db-host (bp.util/get-settings-value settings (conj env-path :database :host))
        db-port (bp.util/get-settings-value settings (conj env-path :database :port))
        db-name (bp.util/get-settings-value settings (conj env-path :database :name))
        db-schema (bp.util/get-settings-value settings (conj env-path :database :schema))
        db-username (bp.util/get-settings-value settings (conj env-path :database :username))
        db-pwd (bp.util/get-settings-value settings (conj env-path :database :password))
        keycloak-realm (bp.util/get-settings-value settings (conj env-path :realm :name))
        keycloak-app-client-id (bp.util/get-settings-value settings (conj env-path :app-client :id))
        keycloak-admin-username (bp.util/get-settings-value settings (conj env-path :admin :username))
        keycloak-admin-pwd (bp.util/get-settings-value settings (conj env-path :admin :password))
        project-protocol (bp.util/get-settings-value settings [:project :proxy environment :protocol])
        project-domain (bp.util/get-settings-value settings [:project :proxy environment :domain])
        keycloak-uri (bp.util/get-settings-value settings (conj env-path :uri))
        external-keycloak-uri (if (= :container deployment-type)
                                (format "%s://%s/auth" project-protocol project-domain)
                                keycloak-uri)
        internal-keycloak-uri (if (= :container deployment-type)
                                "http://keycloak:8080"
                                keycloak-uri)
        memory-limit (bp.util/get-settings-value settings (conj env-path :memory-limit-mb))]
    ;; Application related environment variables
    {:KEYCLOAK_REALM keycloak-realm
     :KEYCLOAK_URI external-keycloak-uri
     :KEYCLOAK_APP_CLIENT_ID keycloak-app-client-id
     :OIDC_AUDIENCE keycloak-app-client-id
     :OIDC_ISSUER_URL (str external-keycloak-uri "/realms/" keycloak-realm)
     :OIDC_JWKS_URI (str internal-keycloak-uri "/realms/" keycloak-realm "/protocol/openid-connect/certs")

     ;; Keycloak service related environment variables
     :MEMORY_LIMIT_KEYCLOAK (str memory-limit "m")
     :KC_BOOTSTRAP_ADMIN_USERNAME keycloak-admin-username
     :KC_BOOTSTRAP_ADMIN_PASSWORD keycloak-admin-pwd
     :KC_DB "postgres"
     :KC_DB_URL (format "jdbc:postgresql://%s:%s/%s" db-host db-port db-name)
     :KC_DB_SCHEMA db-schema
     :KC_DB_USERNAME db-username
     :KC_DB_PASSWORD db-pwd
     :KC_PROXY_HEADERS "xforwarded"
     :KC_HTTP_ENABLED "true"
     :KC_HOSTNAME external-keycloak-uri
     :KC_HOSTNAME_ADMIN external-keycloak-uri}))

(defn- build-env-variables
  [settings environment]
  (let [base-path [:project :profiles :auth-keycloak :deployment (bp.util/get-env-type environment) :?]
        deployment-type (bp.util/get-settings-value settings (conj base-path :deployment-type))
        env-path (conj base-path :environment environment)]
    (if (= :external deployment-type)
      (build-external-env-variables settings env-path)
      (build-container-env-variables settings environment deployment-type env-path))))

(defn- build-docker-compose-files
  [settings]
  (let [common ["docker-compose.keycloak.yml"]
        common-dev-ci ["docker-compose.keycloak.common-dev-ci.yml"]
        ci ["docker-compose.keycloak.ci.yml"]
        aws ["docker-compose.keycloak.aws.yml"]
        deployment-target (bp.util/get-settings-value settings :deployment-target/value)
        deployment-type-to-dev
        (bp.util/get-settings-value settings :project.profiles.auth-keycloak.deployment.to-develop.?/deployment-type)
        deployment-type-to-dep
        (bp.util/get-settings-value settings :project.profiles.auth-keycloak.deployment.to-deploy.?/deployment-type)]
    (cond->  {:to-develop [] :ci [] :to-deploy []}
      (= :container deployment-type-to-dev)
      (assoc :to-develop (concat common common-dev-ci)
             :ci (concat common common-dev-ci ci))

      (= :container deployment-type-to-dep)
      (assoc :to-deploy common)

      (and (= :aws deployment-target)
           (= :container deployment-type-to-dep))
      (update :to-deploy concat aws))))

(defn- build-docker-files-to-copy
  [settings]
  (bp.util/build-profile-docker-files-to-copy
   (build-docker-compose-files settings)
   "authentication/keycloak/"
   [{:src "authentication/keycloak/keycloak" :dst "keycloak"}
    {:src "authentication/keycloak/proxy" :dst "proxy"}]))

(defn- build-profile-env-outputs
  [settings environment]
  (let [env-type (bp.util/get-env-type environment)
        base-path [:project :profiles :auth-keycloak :deployment env-type]
        deployment-choice (bp.util/get-settings-value settings (conj base-path :value))
        deployment-type (bp.util/get-settings-value settings (conj base-path :? :deployment-type))
        env-path (conj base-path :? :environment environment)
        realm-name (bp.util/get-settings-value settings (conj env-path :realm :name))
        project-domain (bp.util/get-settings-value settings [:project :proxy environment :domain])
        project-protocol (bp.util/get-settings-value settings [:project :proxy environment :protocol])
        keycloak-uri (bp.util/get-settings-value settings (conj env-path :uri))
        db-deployment-type (bp.util/get-settings-value settings [:project :profiles :auth-keycloak :deployment env-type :container :db-deployment-type])
        external-uri (if (= :external deployment-type)
                       keycloak-uri
                       (format "%s://%s/auth" project-protocol project-domain))
        internal-uri (if (= :external deployment-type)
                       keycloak-uri
                       "http://keycloak:8080")
        token-url (format "%s/realms/%s/protocol/openid-connect/token" internal-uri realm-name)
        api-url (format "%s/realms/%s/protocol/openid-connect/userinfo" internal-uri realm-name)
        auth-url (format "%s/realms/%s/protocol/openid-connect/auth" external-uri realm-name)
        logout-url (format "%s/realms/%s/protocol/openid-connect/logout" external-uri realm-name)]
    (-> {}
        (assoc-in [:deployment env-type :container :depends-on-postgres?] (= :container db-deployment-type))
        (assoc-in [:deployment env-type deployment-choice :environment environment] {:token-url token-url
                                                                                     :api-url api-url
                                                                                     :auth-url auth-url
                                                                                     :logout-url logout-url}))))

(defmethod registry/pre-render-hook :auth-keycloak
  [_ settings]
  {:dependencies '[[dev.gethop/buddy-auth.jwt-oidc "1.0.3"]
                   [duct/middleware.buddy "0.2.0"]
                   [io.github.cljsjs/keycloak-js "25.0.6-0"]]
   :config-edn {:base (merge
                       (jwt-oidc-config settings)
                       (buddy-auth-config settings)
                       (user-api-config settings))
                :common-config (common-config)
                :config (keycloak-config)
                :api-routes (api-routes settings)}
   :load-frontend-app {:client {:events ["[:dispatch [::init-keycloak]]"]
                                :code [load-frontend-app-client-code]}
                       :routes {:requires [[(symbol (str (bp.util/get-settings-value settings :project/name) ".client.session")) :as (symbol "session")]
                                           [(symbol (str (bp.util/get-settings-value settings :project/name) ".client.view.forbidden")) :as (symbol "view.forbidden")]]
                                :navigated-event [navigated-event-code]}}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files (concat [{:src "authentication/common"}
                   {:src "authentication/keycloak/app" :dst "app"}]
                  (build-docker-files-to-copy settings))
   :docker-compose (build-docker-compose-files settings)
   :deploy-files ["keycloak/themes"]
   :extra-app-docker-compose-environment-variables ["KEYCLOAK_URI"
                                                    "KEYCLOAK_REALM"
                                                    "KEYCLOAK_APP_CLIENT_ID"
                                                    "OIDC_ISSUER_URL"
                                                    "OIDC_AUDIENCE"
                                                    "OIDC_JWKS_URI"]
   :outputs (meta-merge (build-profile-env-outputs settings :dev)
                        (build-profile-env-outputs settings :test)
                        (build-profile-env-outputs settings :prod))})
