;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.cli
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [clojure.pprint :refer [pprint]]
            [hop-cli.bootstrap.main :as main]
            [hop-cli.bootstrap.settings :as settings]
            [hop-cli.bootstrap.settings-editor :as settings-editor]
            [hop-cli.util.error :as error]
            [hop-cli.util.help :as help]))

(defn- bootstrap-handler
  [environments {:keys [opts]}]
  (pprint (main/bootstrap-hop (assoc opts :environments environments))))

(defn- copy-settings-handler
  [{:keys [opts]}]
  (pprint (settings/copy (:settings-file-path opts))))

(defn- open-settings-editor-handler
  [{:keys [opts]}]
  (pprint (settings-editor/serve-settings-editor opts)))

(declare print-help-handler)

(defn- cli-cmd-table
  []
  [{:cmds ["new-project"]
    :fn (partial bootstrap-handler [:dev :test])
    :error-fn error/generic-error-handler
    :desc "Command for bootstrapping a new HOP Application. That includes provisioning the infrastructure and generating the project files."
    :spec {:settings-file-path {:alias :s
                                :desc "Path to the HOP CLI settings file."
                                :require true
                                :validate (comp fs/exists? fs/file)
                                :error-msgs {:validate "Couldn't find HOP settings file. Aborting..."}}
           :target-project-dir {:alias :d
                                :desc "Directory in which the new project will be created."
                                :require true
                                :validate (comp not fs/exists? fs/file)
                                :error-msgs {:validate "Project directory already exists. Please input a different directory."}}}}
   {:cmds ["create-settings-file"]
    :fn copy-settings-handler
    :error-fn error/generic-error-handler
    :desc "Creates a file with a copy of the default HOP CLI configuration."
    :spec {:settings-file-path {:alias :s
                                :desc "Path where the settings file will be copied to."
                                :require true}}}
   {:cmds ["copy-settings"]
    :fn copy-settings-handler
    :error-fn error/generic-error-handler
    :desc "An alias for create-settings-file."
    :spec {:settings-file-path {:alias :s
                                :desc "Path where the settings file will be copied to."
                                :require true}}}
   {:cmds ["prod-infrastructure"]
    :fn (partial bootstrap-handler [:prod])
    :error-fn error/generic-error-handler
    :desc "Command for provisioning the infrastructure for the production environment."
    :spec {:settings-file-path {:alias :s
                                :desc "Path to the HOP CLI settings file. It should be the same file used when bootstrapping the HOP Application."
                                :require true
                                :validate (comp fs/exists? fs/file)
                                :error-msgs {:validate "Couldn't find HOP setting.edn file. Aborting..."}}
           :target-project-dir {:alias :d
                                :desc "Directory where the project is located."
                                :require true
                                :validate (comp fs/exists? fs/file)
                                :error-msgs {:validate "Project directory must exist. Please input a different directory."}}}}
   {:cmds ["open-settings-editor"]
    :fn open-settings-editor-handler
    :error-fn error/generic-error-handler
    :spec {:port {:alias :p
                  :default 8090
                  :desc "Port on which the web server will be launched."
                  :validate pos-int?
                  :coerce :int
                  :error-msgs {:validate "Invalid port number format."
                               :coerce "Port is not a valid number. Aborting... "}}}
    :desc "Opens a web-based wizard for creating or editing the settings file."}
   {:cmds []
    :fn print-help-handler}])

(defn- print-help-handler
  [_]
  (help/print-help (cli-cmd-table) "bootstrap"))

(defn main
  [args]
  (cli/dispatch (cli-cmd-table) args))
