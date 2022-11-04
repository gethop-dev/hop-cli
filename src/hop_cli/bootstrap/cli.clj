(ns hop-cli.bootstrap.cli
  (:require [babashka.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [hop-cli.bootstrap.main :as main]
            [hop-cli.bootstrap.settings :as settings]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(defn- bootstrap-handler
  [{:keys [opts]}]
  (pprint (main/bootstrap-hop (:settings-file-path opts))))

(defn- copy-settings-handler
  [{:keys [opts]}]
  (pprint (settings/copy (:dst opts))))

(declare print-help-handler)

(defn- cli-cmd-table
  []
  [{:cmds ["new-project"]
    :fn bootstrap-handler
    :error-fn error/generic-error-handler
    :desc "Bootstraps a new HOP based project"
    :spec {:settings-file-path {:alias :s
                                :desc "The HOP settings file path."
                                :require true}}}
   {:cmds ["copy-settings"]
    :fn copy-settings-handler
    :error-fn error/generic-error-handler
    :desc "Makes a copy of the default hop-cli configuration file."
    :spec {:dst {:alias :d
                 :desc "Destination file or directory."
                 :require true}}}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) "bootstrap"))

(defn main
  [args]
  (cli/dispatch (cli-cmd-table) args))
