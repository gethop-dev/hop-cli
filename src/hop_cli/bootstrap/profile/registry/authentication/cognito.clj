;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry.authentication.cognito
  (:require [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]))

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
   ::init-cognito
   (fn [{:keys [db]} _]
     {:dispatch [::session/set-config {:oidc (get-in db [:config :cognito])}]}))")

(defn- build-env-variables
  [settings environment]
  (let [base-path [:project :profiles :auth-cognito :environment environment :user-pool :?]
        pool-id (bp.util/get-settings-value settings (conj base-path :id))
        pool-url (bp.util/get-settings-value settings (conj base-path :url))
        client-id (bp.util/get-settings-value settings (conj base-path :app-client :id))]
    {:COGNITO_USER_POOL_ID pool-id
     :OIDC_AUDIENCE client-id
     :OIDC_ISSUER_URL pool-url
     :OIDC_JWKS_URI (str pool-url "/.well-known/jwks.json")}))

(defmethod registry/pre-render-hook :auth-cognito
  [_ settings]
  {:dependencies '[[dev.gethop/session.re-frame.cognito "0.1.1-alpha"]
                   [dev.gethop/user-manager.cognito "0.1.1"]
                   [dev.gethop/buddy-auth.jwt-oidc "1.0.3"]
                   [duct/middleware.buddy "0.2.0"]]
   :config-edn {:base (merge
                       (cognito-adapter-config settings)
                       (jwt-oidc-config settings)
                       (buddy-auth-config settings)
                       (user-api-config settings))
                :common-config (common-config)
                :config (cognito-config)
                :api-routes (api-routes settings)}
   :load-frontend-app {:client {:requires '[[dev.gethop.session.re-frame.cognito :as session]]
                                :events ["[:dispatch [::init-cognito]]"]
                                :code [load-frontend-app-client-code]}}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :extra-app-docker-compose-environment-variables ["OIDC_ISSUER_URL"
                                                    "OIDC_AUDIENCE"
                                                    "OIDC_JWKS_URI"
                                                    "COGNITO_USER_POOL_ID"]
   :files [{:src "authentication/common"}
           {:src "authentication/cognito"}]})
