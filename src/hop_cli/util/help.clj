;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util.help
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [hop-cli.util :as util]))

(defn calc-command-length
  [separator-len cmds]
  (let [cmds-len (reduce #(+ %1 (count %2))
                         0
                         cmds)
        separators-len (* separator-len (dec (count cmds)))]
    (+ cmds-len separators-len)))

(defn- wrap-lines
  "Copied from https://rosettacode.org/wiki/Word_wrap#Clojure"
  [wrap-size text]
  (-> text
      (clojure.string/split #" ")
      (->> (pprint/cl-format nil (str "~{~<~%~1," wrap-size ":;~A~> ~}")))
      (str/split #"\n")))

(defn print-help
  ([cmds]
   (print-help cmds ""))
  ([cmds main-cmd]
   (let [cmd-trailer " "
         cmds-separator " "
         cmds-separator-len (count cmds-separator)
         calc-length (partial calc-command-length cmds-separator-len)
         max-cmd-len (reduce #(max %1 (-> %2 :cmds calc-length)) 0 cmds)
         max-width 78
         desc-separator "  "
         desc-offset (+ (count cmd-trailer) max-cmd-len (count desc-separator))
         desc-trailer (apply str (repeat desc-offset " "))
         desc-width (- max-width desc-offset)]
     (println (format "Version: %s" (util/get-version)))
     (println (format "Usage: %s <subcommand> <options>" main-cmd))
     (println)
     (println "Subcommands")
     (doseq [{:keys [cmds desc]} cmds
             :when (seq cmds)
             :let [format-str (str "  %-" max-cmd-len "s%s%s")
                   cmd (str/join cmds-separator cmds)
                   desc-lines (wrap-lines desc-width desc)]]
       (println (format format-str cmd desc-separator (first desc-lines)))
       (doseq [desc-line (rest desc-lines)]
         (println desc-trailer desc-line))))))
