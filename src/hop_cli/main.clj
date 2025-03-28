;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.main
  (:require [babashka.cli :as cli]
            [hop-cli.aws.cli :as aws.cli]
            [hop-cli.bootstrap.cli :as bootstrap.cli]
            [hop-cli.keycloak.cli :as keycloak.cli]
            [hop-cli.util :as util]
            [hop-cli.util.help :as help]))

(declare print-help-handler)

(defn- cli-cmd-table
  [args]
  [{:cmds ["version"]
    :fn (fn [_] (println (util/get-version)))
    :desc "Get HOP CLI version"}
   {:cmds ["bootstrap"]
    :fn (fn [_] (bootstrap.cli/main (rest args)))
    :desc "HOP Application bootstrap commands"}
   {:cmds ["aws"]
    :fn (fn [_] (aws.cli/main (rest args)))
    :desc "AWS utility commands"}
   {:cmds ["keycloak"]
    :fn (fn [_] (keycloak.cli/main (rest args)))
    :desc "Keycloak utility commands"}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table nil)))

(defn -main
  [& args]
  (let [args (into [] args)]
    (try
      (cli/dispatch (cli-cmd-table args) args)
      (catch clojure.lang.ExceptionInfo e
        (println "Incomplete or incorrect command requested. See help below.\n")
        (let [{:keys [cause all-commands]} (ex-data e)
              help-cmd [(first args) "--help"]]
          (when (and (get #{:input-exhausted :no-match} cause)
                     (seq all-commands))
            (cli/dispatch (cli-cmd-table help-cmd) help-cmd)))))))
