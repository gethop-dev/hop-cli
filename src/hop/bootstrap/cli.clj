(ns hop.bootstrap.cli
  (:require [babashka.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [hop.bootstrap.main :as main]
            [hop.util.error :as error]
            [hop.util.help :as help]))

(def common-cli-spec
  {:settings-file-path {:alias :s
                        :desc "The HOP settings file path."
                        :require true}})

(def cli-spec
  {:bootstrap (select-keys common-cli-spec [:settings-file-path])})

(defn- bootstrap-handler
  [{:keys [opts]}]
  (pprint (main/bootstrap-hop (:settings-file-path opts))))

(declare print-help-handler)

(defn- cli-cmd-table
  []
  [{:cmds ["bootstrap"]
    :fn bootstrap-handler
    :spec (get cli-spec :bootstrap)
    :error-fn error/generic-error-handler
    :desc "Bootstraps a new HOP based project"}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table)))

(defn -main [& args]
  (cli/dispatch (cli-cmd-table) args))
