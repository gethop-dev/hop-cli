;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.cli
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [clojure.pprint :refer [pprint]]
            [hop-cli.bootstrap.main :as main]
            [hop-cli.bootstrap.settings :as settings]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(defn- bootstrap-handler
  [environments {:keys [opts]}]
  (pprint (main/bootstrap-hop (assoc opts :environments environments))))

(defn- copy-settings-handler
  [{:keys [opts]}]
  (pprint (settings/copy (:settings-file-path opts))))

(declare print-help-handler)

(defn- cli-cmd-table
  []
  [{:cmds ["new-project"]
    :fn (partial bootstrap-handler [:dev :test])
    :error-fn error/generic-error-handler
    :desc "Bootstraps a new HOP based project"
    :spec {:settings-file-path {:alias :s
                                :desc "The HOP settings file path."
                                :require true
                                :validate (comp fs/exists? fs/file)
                                :error-msgs {:validate "Couldn't find HOP setting.edn file. Aborting..."}}
           :target-project-dir {:alias :d
                                :desc "Target directory to create the new project."
                                :require true
                                :validate (comp not fs/exists? fs/file)
                                :error-msgs {:validate "Project directory already exists. Please input a different directory."}}}}
   {:cmds ["copy-settings"]
    :fn copy-settings-handler
    :error-fn error/generic-error-handler
    :desc "Makes a copy of the default hop-cli configuration file."
    :spec {:settings-file-path {:alias :s
                                :desc "Destination file."
                                :require true}}}
   {:cmds ["prod-infrastructure"]
    :fn (partial bootstrap-handler [:prod])
    :error-fn error/generic-error-handler
    :desc "Provision a production cloud infrastructure."
    :spec {:settings-file-path {:alias :s
                                :desc "The HOP settings file path."
                                :require true
                                :validate (comp fs/exists? fs/file)
                                :error-msgs {:validate "Couldn't find HOP setting.edn file. Aborting..."}}
           :target-project-dir {:alias :d
                                :desc "Directory where the project is located."
                                :require true
                                :validate (comp fs/exists? fs/file)
                                :error-msgs {:validate "Project directory must exist. Please input a different directory."}}}}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) "bootstrap"))

(defn main
  [args]
  (cli/dispatch (cli-cmd-table) args))
