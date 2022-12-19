;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util.error
  (:require [babashka.cli :as cli]))

(defn get-error-msg
  [{:keys [spec cause msg option]}]
  (or (get-in spec [option :error-msgs cause]) msg))

(defn generic-error-handler
  [{:keys [spec] :as error}]
  (println "An error has occurred:" (get-error-msg error))
  (println "Usage:")
  (println (cli/format-opts {:spec spec}))
  (System/exit 1))
