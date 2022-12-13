(ns hop-cli.bootstrap.profile.registry-loader
  (:require [hop-cli.bootstrap.profile.registry.authentication.cognito]
            [hop-cli.bootstrap.profile.registry.authentication.keycloak]
            [hop-cli.bootstrap.profile.registry.aws]
            [hop-cli.bootstrap.profile.registry.bi.grafana]
            [hop-cli.bootstrap.profile.registry.ci]
            [hop-cli.bootstrap.profile.registry.core]
            [hop-cli.bootstrap.profile.registry.frontend]
            [hop-cli.bootstrap.profile.registry.object-storage.s3]
            [hop-cli.bootstrap.profile.registry.observability.runtime-configurable-logging]
            [hop-cli.bootstrap.profile.registry.persistence.sql]))

(def ^:const profile-list
  "List of the HOP profiles in the prefered order of execution"
  [:core :persistence-sql :auth-cognito :auth-keycloak :bi-grafana
   :frontend :observability-runtime-configurable-logging
   :observability-cloudwatch-logging :aws :object-storage-s3 :ci])
