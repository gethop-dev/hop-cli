(ns hop-cli.bootstrap.profile.persistence)

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

(defn- ragtime-config
  [settings]
  {[:duct.migrator/ragtime (keyword (format "%s/prod" (:project/name settings)))]
   {:database (tagged-literal 'ig/ref :duct.database/sql)
    :logger (tagged-literal 'ig/ref :duct/logger)
    :strategy :raise-error
    :migrations-table "ragtime_migrations"
    :migrations []}})

(defn profile
  [settings]
  {:dependencies '[[duct/module.sql "0.6.1"]
                   [dev.gethop/sql-utils "0.4.13"]
                   [org.postgresql/postgresql "42.3.3"]]
   :config-edn {:base (merge (sql-config settings)
                             (hikaricp-config settings)
                             (ragtime-config settings))}
   :files [{:files [{:src "resources/bootstrap/project/persistence"
                     :dst "new-project"}]
            :copy-if :project/persistence?}]})
