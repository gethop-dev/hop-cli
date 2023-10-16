;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/
{{=<< >>=}}
(ns <<project.name>>.duct.env
  (:require [clojure.edn :as edn]
            [duct.core.env :as env]))

(defmethod env/coerce 'Keyword [^String x _]
  (if (.startsWith x ":")
    (keyword (subs x 1))
    (keyword x)))

(defmethod env/coerce 'Edn [^String x _]
  (edn/read-string x))
