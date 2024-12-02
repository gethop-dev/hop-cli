;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.time
  (:require [cljc.java-time.format.date-time-formatter :as jt.formatter]
            [cljc.java-time.instant :as jt.instant]
            [cljc.java-time.zone-id :as jt.zone-id]))

(defn instant->zoned-date-time-string
  [instant {:keys [format-pattern zone-id]
            :or {format-pattern "yyyy-MM-dd HH:dd"
                 zone-id (jt.zone-id/system-default)}}]
  (let [formatter (jt.formatter/of-pattern format-pattern)
        zoned-date-time (jt.instant/at-zone instant zone-id)]
    (jt.formatter/format formatter zoned-date-time)))
