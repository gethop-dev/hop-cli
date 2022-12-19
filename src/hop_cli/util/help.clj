;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util.help
  (:require [clojure.string :as str]))

(defn calc-command-length
  [cmds]
  (reduce #(+ %1 (count %2)) 0 cmds))

(defn print-help
  ([cmds]
   (print-help cmds ""))
  ([cmds main-cmd]
   (let [max-cmd-len (reduce #(max %1 (-> %2 :cmds calc-command-length)) 0 cmds)]
     (println (format "Usage: %s <subcommand> <options>" main-cmd))
     (println)
     (println "Subcommands")
     (doseq [{:keys [cmds desc]} cmds
             :when (seq cmds)
             :let [format-str (str "  %-" max-cmd-len "s  %s")
                   cmd (str/join " "  cmds)]]
       (println (format format-str cmd desc))))))
