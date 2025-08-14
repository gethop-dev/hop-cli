;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.middleware.cors
  (:require [clojure.string :as str]
            [integrant.core :as ig]
            [ring.middleware.cors :as mid.cors]))

(defmethod ig/init-key :<<project.name>>.api.middleware/cors
  [_ {:keys [allowed-origins allowed-methods]}]
  (cond-> [mid.cors/wrap-cors]
    (seq allowed-origins)
    (into [:access-control-allow-origin (->> (str/split allowed-origins #",")
                                             (mapv re-pattern))] )
    (seq allowed-methods)
    (into [:access-control-allow-methods allowed-methods])))


