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

(defn- build-env-variables
  [_settings _environment]
  {:JDBC_DATABASE_URL ""})

(defn profile
  [settings]
  {:dependencies '[[duct/module.sql "0.6.1"]
                   [dev.gethop/sql-utils "0.4.13"]
                   [org.postgresql/postgresql "42.3.3"]]
   :config-edn {:base (merge (sql-config settings)
                             (hikaricp-config settings)
                             (ragtime-config settings))
                :dev (dev-ragtime-config settings)}
   :environment-variables {:dev (build-env-variables settings :dev)
                           :test (build-env-variables settings :test)
                           :prod (build-env-variables settings :prod)}
   :files [{:src "persistence/sql"}]})
