(ns hop-cli.bootstrap.profile.registry.persistence.sql
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.util :as bp.util]
            [meta-merge.core :refer [meta-merge]]))

(defn- sql-config
  [settings]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    {[(keyword (format "%s.boundary.adapter.persistence/sql" project-name))
      (keyword (format "%s.boundary.adapter.persistence/postgres" project-name))]
     (tagged-literal 'ig/ref :duct.database/sql)}))

(defn- hikaricp-config
  [_]
  {:duct.database.sql/hikaricp
   {:adapter (tagged-literal 'duct/env ["APP_DB_TYPE" 'Str])
    :server-name (tagged-literal 'duct/env ["APP_DB_HOST" 'Str])
    :port-number (tagged-literal 'duct/env ["APP_DB_PORT" 'Str])
    :database-name (tagged-literal 'duct/env ["APP_DB_NAME" 'Str])
    :username (tagged-literal 'duct/env ["APP_DB_USER" 'Str])
    :password (tagged-literal 'duct/env ["APP_DB_PASSWORD" 'Str])
    :re-write-batched-inserts true
    :logger nil
    :minimum-idle 10
    :maximum-pool-size 25}})

(defn- build-ragtime-config-key
  [settings environment]
  (let [project-name (bp.util/get-settings-value settings :project/name)]
    [:duct.migrator/ragtime
     (keyword (format "%s/%s" project-name (name environment)))]))

(defn- ragtime-config
  [settings]
  {(build-ragtime-config-key settings :prod)
   {:database (tagged-literal 'ig/ref :duct.database/sql)
    :logger (tagged-literal 'ig/ref :duct/logger)
    :strategy :raise-error
    :migrations-table "ragtime_migrations"
    :migrations []}})

(defn- dev-ragtime-config
  [settings]
  {(build-ragtime-config-key settings :dev)
   {:database (tagged-literal 'ig/ref :duct.database/sql)
    :logger (tagged-literal 'ig/ref :duct/logger)
    :strategy :raise-error
    :migrations-table "ragtime_migrations_dev"
    :fake-dependency-to-force-initialization-order
    (build-ragtime-config-key settings :prod)
    :migrations []}})

(defn- build-env-variables
  [settings environment]
  (let [base-path [:project :profiles :persistence-sql :deployment (bp.util/get-env-type environment) :?]
        env-path (conj base-path :environment environment)
        deploy-type (bp.util/get-settings-value settings (conj base-path :deployment-type))
        host (if (= :container deploy-type)
               "postgres"
               (bp.util/get-settings-value settings (conj env-path :database :host)))
        type (if (= :container deploy-type)
               "postgresql"
               (bp.util/get-settings-value settings (conj env-path :database :type)))
        port (bp.util/get-settings-value settings (conj env-path :database :port))
        db (bp.util/get-settings-value settings (conj env-path :database :name))
        app-user (bp.util/get-settings-value settings (conj env-path :database :app-user :username))
        app-password (bp.util/get-settings-value settings (conj env-path :database :app-user :password))
        app-schema (bp.util/get-settings-value settings (conj env-path :database :app-user :schema))
        admin-user (bp.util/get-settings-value settings (conj env-path :database :admin-user :username))
        admin-password (bp.util/get-settings-value settings (conj env-path :database :admin-user :password))]
    {:APP_DB_TYPE type
     :APP_DB_HOST host
     :APP_DB_PORT port
     :APP_DB_NAME db
     :APP_DB_USER app-user
     :APP_DB_PASSWORD app-password
     :APP_DB_SCHEMA app-schema
     :DB_PORT port
     :DB_NAME db
     :DB_ADMIN_USER admin-user
     :DB_ADMIN_PASSWORD admin-password}))

(defn- build-docker-compose-files
  [settings]
  (let [common ["docker-compose.postgres.yml"]
        common-dev-ci ["docker-compose.postgres.common-dev-ci.yml"]
        ci ["docker-compose.postgres.ci.yml"]]
    (cond->  {:to-develop [] :ci [] :to-deploy []}
      (= :container (bp.util/get-settings-value settings :project.profiles.persistence-sql.deployment.to-develop.?/deployment-type))
      (assoc :to-develop (concat common common-dev-ci)
             :ci (concat common common-dev-ci ci)))))

(defn- build-docker-files-to-copy
  [settings]
  (bp.util/build-profile-docker-files-to-copy
   (build-docker-compose-files settings)
   "persistence/sql/"
   []))

(defn- build-profile-env-outputs
  [settings env]
  (when (and
         (= :dev env)
         (= :container (bp.util/get-settings-value settings :project.profiles.persistence-sql.deployment.to-develop/value)))
    {:deployment
     {:to-develop
      {:container
       {:environment
        {:dev
         {:database {:host "postgres"}}}}}}}))

(defn- replace-env-variable
  [settings environment [env-var-str env-var-name]]
  (let [path [:project :environment-variables environment (keyword env-var-name)]
        env-var-value (bp.util/get-settings-value settings path)]
    (if env-var-value
      env-var-value
      env-var-str)))

(defn build-environment-init-db-sql-string
  [settings environment]
  (let [project-dir (bp.util/get-settings-value settings :project/target-dir)
        template-file (format "%s/postgres/init-scripts/%s/%s/01_create_schemas_and_roles.sql"
                              project-dir
                              (name (bp.util/get-env-type environment))
                              (name environment))
        template-content (slurp (fs/file template-file))]
    (str/replace template-content #"\$\{([^}]+)\}" #(replace-env-variable settings environment %1))))

(defn build-init-db-sql-string
  [settings]
  (with-out-str
    (println "Once the DB is up and running you need to run the following SQL statements:")
    (println "\nFor test env:\n")
    (println (build-environment-init-db-sql-string settings :test))
    (println "\nFor prod env:\n")
    (println (build-environment-init-db-sql-string settings :prod))
    (println "The scripts are stored in the project under postgres/init-scripts for reference.")))

(defmethod registry/pre-render-hook :persistence-sql
  [_ settings]
  {:dependencies '[[duct/migrator.ragtime "0.3.2"]
                   [dev.gethop/database.sql.hikaricp "0.4.1"]
                   [dev.gethop/sql-utils "0.4.13"]
                   [org.postgresql/postgresql "42.3.3"]]
   :config-edn {:base (merge (sql-config settings)
                             (hikaricp-config settings)
                             (ragtime-config settings))
                :dev (dev-ragtime-config settings)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files (concat [{:src "persistence/sql/app"
                    :dst "app"}
                   {:src "persistence/sql/postgres/init-scripts/to-deploy"
                    :dst "postgres/init-scripts/to-deploy"}
                   {:src "persistence/sql/postgres/init-scripts/to-develop/dev"
                    :dst "postgres/init-scripts/to-develop/dev"}]
                  (build-docker-files-to-copy settings))
   :docker-compose (build-docker-compose-files settings)
   :extra-app-docker-compose-environment-variables ["APP_DB_TYPE"
                                                    "APP_DB_HOST"
                                                    "APP_DB_PORT"
                                                    "APP_DB_NAME"
                                                    "APP_DB_USER"
                                                    "APP_DB_PASSWORD"]
   :outputs (meta-merge
             (build-profile-env-outputs settings :dev)
             (build-profile-env-outputs settings :test)
             (build-profile-env-outputs settings :prod))})

(defmethod registry/post-render-hook :persistence-sql
  [_ settings]
  {:post-installation-messages [(build-init-db-sql-string settings)]})
