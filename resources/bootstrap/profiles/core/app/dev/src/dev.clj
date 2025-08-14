;; This Source Code Form is subject to the terms of the MIT license.
;; If a copy of the MIT license was not distributed with this
;; file, You can obtain one at https://opensource.org/licenses/MIT
{{=<< >>=}}
(ns dev
  {:clj-kondo/config '{:linters {:refer-all {:exclude #{clojure.repl}}
                                 :unused-referred-var {:level :off}
                                 :unused-namespace {:level :off}}}}
  (:refer-clojure :exclude [test])
  (:require <<#project.dev-requires>><<&.>><</project.dev-requires>>
            [<<project.name>>.duct.env]
            [clojure.java.io :as io]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl :refer [auto-reset]]
            [fipp.edn :refer [pprint]]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [integrant.repl.state :refer [config system]]))

(duct/load-hierarchy)

(defn read-config []
  (duct/read-config (io/resource "<<project.files-name>>/config.edn")))

(def profiles
  [:duct.profile/dev :duct.profile/local])

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
