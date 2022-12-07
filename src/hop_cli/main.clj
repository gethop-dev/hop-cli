(ns hop-cli.main
  (:require [babashka.cli :as cli]
            [hop-cli.aws.cli :as aws.cli]
            [hop-cli.bootstrap.cli :as bootstrap.cli]
            [hop-cli.keycloak.cli :as keycloak.cli]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(declare print-help-handler)

(defn- cli-cmd-table
  [args]
  [{:cmds ["bootstrap"]
    :fn (fn [_] (bootstrap.cli/main (rest args)))
    :error-fn error/generic-error-handler
    :desc "HOP bootstrap commands"}
   {:cmds ["aws"]
    :fn (fn [_] (aws.cli/main (rest args)))
    :error-fn error/generic-error-handler
    :desc "AWS utility commands"}
   {:cmds ["keycloak"]
    :fn (fn [_] (keycloak.cli/main (rest args)))
    :error-fn error/generic-error-handler
    :desc "Keycloak utility commands"}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table nil)))

(defn -main
  [& args]
  (cli/dispatch (cli-cmd-table args) args))
