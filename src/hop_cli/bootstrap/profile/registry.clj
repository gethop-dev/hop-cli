(ns hop-cli.bootstrap.profile.registry
  (:require [hop-cli.bootstrap.profile.registry.authentication.cognito :as auth.cognito]
            [hop-cli.bootstrap.profile.registry.authentication.keycloak :as auth.keycloak]
            [hop-cli.bootstrap.profile.registry.aws :as aws]
            [hop-cli.bootstrap.profile.registry.bi.grafana :as bi.grafana]
            [hop-cli.bootstrap.profile.registry.core :as core]
            [hop-cli.bootstrap.profile.registry.frontend :as frontend]
            [hop-cli.bootstrap.profile.registry.persistence.sql :as p.sql]
            [hop-cli.util :as util]))

(def profiles
  {:auth-cognito auth.cognito/profile
   :auth-keycloak  auth.keycloak/profile
   :aws aws/profile
   :bi-grafana bi.grafana/profile
   :core core/profile
   :frontend frontend/profile
   :persistence-sql p.sql/profile})

(defn- merge-profile-key
  [k v1 v2]
  (cond
    (get #{:dependencies :files} k)
    (vec (concat v1 v2))

    (get #{:environment-variables :config-edn} k)
    (merge-with merge v1 v2)

    (get #{:load-frontend-app} k)
    (merge-with concat v1 v2)

    :else
    (merge v1 v2)))

(defn get-selected-profiles-data
  [settings]
  (let [profile-kws (cons :core (get settings :project/profiles))]
    (->> (select-keys profiles profile-kws)
         (vals)
         (map (fn [profile-fn] (profile-fn settings)))
         (apply util/merge-with-key merge-profile-key))))
