;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util.error
  (:require [hop-cli.util.help :as help]))

(defn get-error-msg
  [{:keys [spec cause msg option]}]
  (or (get-in spec [option :error-msgs cause]) msg))

(defn generic-error-handler
  [cmd-path cmd]
  (when-not (-> cmd :opts :help)
    (println "An error has occurred:" (get-error-msg cmd))
    (println))
  (help/print-cmd-help cmd-path cmd)
  (if (-> cmd :opts :help)
    (System/exit 0)
    (System/exit 1)))
