;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.service.health<<#project.profiles.persistence-sql.enabled>>
  (:require [<<project.name>>.boundary.port.persistence :as persistence]<</project.profiles.persistence-sql.enabled>>))

(defn get-health<<#project.profiles.persistence-sql.enabled>>
  [{:keys [p-adapter] :as _config}]<</project.profiles.persistence-sql.enabled>><<^project.profiles.persistence-sql.enabled>>
  [_config]<</project.profiles.persistence-sql.enabled>>
  (let [health-checks<<#project.profiles.persistence-sql.enabled>>{:main-db-connection (persistence/get-status p-adapter {})}<</project.profiles.persistence-sql.enabled>><<^project.profiles.persistence-sql.enabled>>{:healthy? true}<</project.profiles.persistence-sql.enabled>>]
    {:healthy? (every? :healthy? (vals health-checks))
     :details health-checks}))
