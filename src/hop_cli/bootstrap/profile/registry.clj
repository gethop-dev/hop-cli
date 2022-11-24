(ns hop-cli.bootstrap.profile.registry
  (:require [hop-cli.bootstrap.profile.registry.authentication.cognito :as auth.cognito]
            [hop-cli.bootstrap.profile.registry.authentication.keycloak :as auth.keycloak]
            [hop-cli.bootstrap.profile.registry.aws :as aws]
            [hop-cli.bootstrap.profile.registry.bi.grafana :as bi.grafana]
            [hop-cli.bootstrap.profile.registry.ci :as ci]
            [hop-cli.bootstrap.profile.registry.core :as core]
            [hop-cli.bootstrap.profile.registry.frontend :as frontend]
            [hop-cli.bootstrap.profile.registry.object-storage.s3 :as os.s3]
            [hop-cli.bootstrap.profile.registry.persistence.sql :as p.sql]))

(def profiles
  [{:kw :core :exec-fn core/profile}
   {:kw :persistence-sql :exec-fn p.sql/profile}
   {:kw :auth-cognito :exec-fn auth.cognito/profile}
   {:kw :auth-keycloak :exec-fn auth.keycloak/profile}
   {:kw :bi-grafana :exec-fn bi.grafana/profile}
   {:kw :frontend :exec-fn frontend/profile}
   {:kw :aws :exec-fn aws/profile}
   {:kw :object-storage-s3 :exec-fn os.s3/profile}
   {:kw :ci :exec-fn ci/profile}])
