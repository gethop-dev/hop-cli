;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry.bi.grafana
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

(defn- dashboard-manager-adapter-config
  [_settings]
  {:dev.gethop.dashboard-manager/grafana
   {:uri (tagged-literal 'duct/env ["GRAFANA_URI" 'Str])
    :credentials [(tagged-literal 'duct/env ["GRAFANA_USERNAME" 'Str])
                  (tagged-literal 'duct/env ["GRAFANA_TEST_PASSWORD" 'Str])]}})

(defn- sso-apps-config
  []
  {:sso-apps [{:name (tagged-literal 'duct/env ["OIDC_SSO_APP_1_NAME" 'Str])
               :login-url (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGIN_URL" 'Str])
               :login-method (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGIN_METHOD" 'Str])
               :logout-url (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGOUT_URL" 'Str])
               :logout-method (tagged-literal 'duct/env ["OIDC_SSO_APP_1_LOGOUT_METHOD" 'Str])}]})

(defn- build-external-env-variables
  [settings env-path]
  (let [grafana-uri
        (bp.util/get-settings-value settings (conj env-path :uri))
        integration-username
        (bp.util/get-settings-value settings (conj env-path :app-integration :username))
        integration-password
        (bp.util/get-settings-value settings (conj env-path :app-integration :password))
        single-sign-on?
        (bp.util/get-settings-value settings (conj env-path :sso))]
    (cond->
     {:DS_MANAGER_URI grafana-uri
      :DS_MANAGER_CREDENTIALS_USER integration-username
      :DS_MANAGER_CREDENTIALS_PASSWORD integration-password}

      single-sign-on?
      (assoc
       :OIDC_SSO_APP_1_NAME "grafana"
       :OIDC_SSO_APP_1_LOGIN_METHOD "GET"
       :OIDC_SSO_APP_1_LOGIN_URL (format "%s/login" grafana-uri)
       :OIDC_SSO_APP_1_LOGOUT_METHOD "GET"
       :OIDC_SSO_APP_1_LOGOUT_URL (format "%s/logout" grafana-uri)))))

(defn- build-container-env-variables
  [settings env-path]
  (cond->
   {;; Adapter configuration
    :DS_MANAGER_URI "http://grafana:4000"
    :DS_MANAGER_CREDENTIALS_USER
    (bp.util/get-settings-value settings (conj env-path :app-integration :username))
    :DS_MANAGER_CREDENTIALS_PASSWORD
    (bp.util/get-settings-value settings (conj env-path :app-integration :password))

    ;; Docker
    :MEMORY_LIMIT_GRAFANA
    (str (bp.util/get-settings-value settings (conj env-path :memory-limit-mb)) "m")
    ;; General settings
    :GF_SECURITY_ALLOW_EMBEDDING "false"
    :GF_SERVER_DOMAIN
    (bp.util/get-settings-value settings (conj env-path :server-domain))
    :GF_SERVER_HTTP_PORT "4000"
    :GF_SERVER_ROOT_URL "%(protocol)s://%(domain)s/grafana/"
    :GF_SERVER_SERVE_FROM_SUB_PATH "true"
    :GF_SNAPSHOTS_EXTERNAL_ENABLED "false"
    :GF_AUTH_ANONYMOUS_ENABLED "false"

    ;; Admin user
    :GF_SECURITY_ADMIN_USER
    (bp.util/get-settings-value settings (conj env-path :admin-user :username))
    :GF_SECURITY_ADMIN_PASSWORD
    (bp.util/get-settings-value settings (conj env-path :admin-user :password))

    ;; Database settings
    :GF_DATABASE_SSL_MODE "disable"
    :GF_DATABASE_TYPE "postgres"
    :GF_DATABASE_HOST
    (format "%s:%s"
            (bp.util/get-settings-value settings (conj env-path :database :host))
            (bp.util/get-settings-value settings (conj env-path :database :port)))
    :GF_DATABASE_NAME
    (bp.util/get-settings-value settings (conj env-path :database :name))
    :GF_DATABASE_USER
    (bp.util/get-settings-value settings (conj env-path :database :username))
    :GF_DATABASE_PASSWORD
    (bp.util/get-settings-value settings (conj env-path :database :password))
    :GF_DATABASE_SCHEMA
    (bp.util/get-settings-value settings (conj env-path :database :schema))

    ;;OIDC
    :GF_AUTH_GENERIC_OAUTH_TOKEN_URL
    (bp.util/get-settings-value settings (conj env-path :oidc :? :token-url))
    :GF_AUTH_GENERIC_OAUTH_API_URL
    (bp.util/get-settings-value settings (conj env-path :oidc :? :api-url))
    :GF_AUTH_GENERIC_OAUTH_AUTH_URL
    (bp.util/get-settings-value settings (conj env-path :oidc :? :auth-url))
    :GF_AUTH_SIGNOUT_REDIRECT_URL
    (bp.util/get-settings-value settings (conj env-path :oidc :? :logout-url))
    :GF_AUTH_GENERIC_OAUTH_CLIENT_ID
    (bp.util/get-settings-value settings (conj env-path :oidc :? :client-id))
    :GF_AUTH_GENERIC_OAUTH_CLIENT_SECRET
    (bp.util/get-settings-value settings (conj env-path :oidc :? :client-secret))
    :GF_AUTH_GENERIC_OAUTH_ENABLED "true"
    :GF_AUTH_GENERIC_OAUTH_ALLOW_SIGN_UP "false"
    :GF_AUTH_GENERIC_OAUTH_SCOPES "email openid"
    :GF_AUTH_LOGIN_COOKIE_NAME "grafana_session_cookie"
    :GF_AUTH_DISABLE_SIGNOUT_MENU "true"
    :GF_AUTH_GENERIC_OAUTH_SIGN_UP "false"}

    ;; Single sign on
    (bp.util/get-settings-value settings (conj env-path :sso))
    (assoc :GF_AUTH_OAUTH_AUTO_LOGIN "true"
           :OIDC_SSO_APP_1_NAME "grafana"
           :OIDC_SSO_APP_1_LOGIN_METHOD "GET"
           :OIDC_SSO_APP_1_LOGIN_URL "/grafana/login"
           :OIDC_SSO_APP_1_LOGOUT_METHOD "GET"
           :OIDC_SSO_APP_1_LOGOUT_URL "/grafana/logout")))

(defn- build-env-variables
  [settings environment]
  (let [env-type (bp.util/get-env-type environment)
        base-path [:project :profiles :bi-grafana :deployment env-type :?]
        deployment-type (bp.util/get-settings-value settings (conj base-path :deployment-type))
        env-path (conj base-path :environment environment)]
    (if (= :external deployment-type)
      (build-external-env-variables settings env-path)
      (build-container-env-variables settings env-path))))

(defn- build-docker-compose-files
  [settings]
  (let [common ["docker-compose.grafana.yml"]
        common-dev-ci ["docker-compose.grafana.common-dev-ci.yml"]
        ci ["docker-compose.grafana.ci.yml"]]
    (cond->  {:to-develop [] :ci [] :to-deploy []}
      (= :container
         (bp.util/get-settings-value settings :project.profiles.bi-grafana.deployment.to-develop.?/deployment-type))
      (assoc :to-develop (concat common common-dev-ci)
             :ci (concat common common-dev-ci ci))

      (= :container
         (bp.util/get-settings-value settings :project.profiles.bi-grafana.deployment.to-deploy.?/deployment-type))
      (assoc :to-deploy common))))

(defn- build-docker-files-to-copy
  [settings]
  (bp.util/build-profile-docker-files-to-copy
   (build-docker-compose-files settings)
   "bi/grafana/"
   [{:src "bi/grafana/grafana" :dst "grafana"}
    {:src "bi/grafana/proxy" :dst "proxy"}]))

(defn- build-dev-outputs
  [settings]
  {:deployment
   {:to-develop
    {:container
     {:depends-on-postgres?
      (= :container
         (bp.util/get-settings-value settings
                                     :project.profiles.bi-grafana.deployment.to-develop.container/db-deployment-type))}}}})

(defmethod registry/pre-render-hook :bi-grafana
  [_ settings]
  {:dependencies '[[dev.gethop/dashboard-manager.grafana "0.2.8"]]
   :config-edn {:base (dashboard-manager-adapter-config settings)
                :config (sso-apps-config)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files (build-docker-files-to-copy settings)
   :docker-compose (build-docker-compose-files settings)
   :extra-app-docker-compose-environment-variables ["OIDC_SSO_APP_1_NAME"
                                                    "OIDC_SSO_APP_1_LOGIN_URL"
                                                    "OIDC_SSO_APP_1_LOGIN_METHOD"
                                                    "OIDC_SSO_APP_1_LOGOUT_URL"
                                                    "OIDC_SSO_APP_1_LOGOUT_METHOD"]
   :outputs (build-dev-outputs settings)})
