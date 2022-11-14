(ns hop-cli.bootstrap.profile.persistence.sql)

(defn- sql-config
  [settings]
  (let [project-name (get settings :project/name)]
    {[(keyword (format "%s.boundary.adapter.persistence/sql" project-name))
      (keyword (format "%s.boundary.adapter.persistence/postgres" project-name))]
     (tagged-literal 'ig/ref :duct.database/sql)}))

(defn- hikaricp-config
  [_]
  {:duct.database.sql/hikaricp
   {:jdbc-url (tagged-literal 'duct/env ["JDBC_DATABASE_URL" 'Str])
    :logger nil
    :minimum-idle 10
    :maximum-pool-size 25}})

(defn- build-ragtime-config-key
  [settings environment]
  [:duct.migrator/ragtime
   (keyword (format "%s/%s" (:project/name settings) (name environment)))])

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

(defn- build-jdbc-database-url
  [{:keys [host port db user password]}]
  (format
   "jdbc:postgresql://%s:%s/%s?user=%s&password=%s&reWriteBatchedInserts=true"
   host port db user password))

(defn- build-dev-env-variables
  [settings]
  (let [base-path "project.profiles.persistence.sql.dev"]
    {:POSTGRES_HOST "db"
     :POSTGRES_PORT "5432"
     :POSTGRES_DB (get settings (keyword base-path "name"))
     :POSTGRES_USER (get settings (keyword (str base-path ".admin/username")))
     :POSTGRES_PASSWORD (get settings (keyword (str base-path ".admin/password")))
     :JDBC_DATABASE_URL
     (build-jdbc-database-url
      {:host "db"
       :port "5432"
       :db (get settings (keyword base-path "name"))
       :user (get settings (keyword (str base-path ".app/username")))
       :password (get settings (keyword (str base-path ".app/password")))})}))

(defn- build-test-prod-env-variables
  [settings environment]
  (let [base-path (str "project.profiles.persistence.sql." (name environment))]
    {:JDBC_DATABASE_URL
     (build-jdbc-database-url
      {:host nil
       :port nil
       :db nil
       :user (get settings (keyword (str base-path ".app/username")))
       :password (get settings (keyword (str base-path ".app/password")))})}))

(defn profile
  [settings]
  {:dependencies '[[duct/module.sql "0.6.1"]
                   [dev.gethop/sql-utils "0.4.13"]
                   [org.postgresql/postgresql "42.3.3"]]
   :config-edn {:base (merge (sql-config settings)
                             (hikaricp-config settings)
                             (ragtime-config settings))
                :dev (dev-ragtime-config settings)}
   :environment-variables {:dev (build-dev-env-variables settings)
                           :test (build-test-prod-env-variables settings :test)
                           :prod (build-test-prod-env-variables settings :prod)}
   :files [{:src "persistence/sql"}]
   :docker-compose {:dev ["docker-compose.common-dev-ci.db.yml"]
                    :ci ["docker-compose.common-dev-ci.db.yml"
                         "docker-compose.ci.db.yml"]}})
