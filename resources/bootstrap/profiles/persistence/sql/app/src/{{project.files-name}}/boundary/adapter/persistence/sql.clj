;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.boundary.adapter.persistence.sql
  (:require [integrant.core :as ig]
            [<<project.name>>.boundary.adapter.persistence.connector :as connector]
            [<<project.name>>.boundary.adapter.persistence.sql.health]
            [<<project.name>>.boundary.adapter.persistence.sql.jdbc-util :as sql.jdbc-util]))

(defn init-sql-adapter
  [{:keys [db-spec logger]}]
  (connector/->Sql (sql.jdbc-util/with-options+logging db-spec logger) logger))

(defmethod ig/init-key :<<project.name>>.boundary.adapter.persistence/sql
  [_ {:keys [duct-adapter logger]}]
  (init-sql-adapter {:db-spec (:spec duct-adapter) :logger logger}))
