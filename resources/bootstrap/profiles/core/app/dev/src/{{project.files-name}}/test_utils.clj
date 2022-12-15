;; This Source Code Form is subject to the terms of the MIT license.
;; If a copy of the MIT license was not distributed with this
;; file, You can obtain one at https://opensource.org/licenses/MIT

{{=<< >>=}}
(ns <<project.name>>.test-utils
  (:require [dev :as dev]
            [duct.core :as duct]
            [integrant.core :as ig]))

(def system (atom {}))

(defn init-handler-with-system
  [ks-to-init]
  (duct/load-hierarchy)
  (-> (dev/read-config)
      (duct/prep-config)
      (ig/init ks-to-init)))

(defn halt-system
  [system]
  (ig/halt! system))

(defn init-halt-system
  [ks-to-init f]
  (reset! system (init-handler-with-system ks-to-init))
  (f)
  (halt-system @system))
