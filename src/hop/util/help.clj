(ns hop.util.help)

(defn print-help
  [cmds]
  (let [max-cmd-len (reduce #(max %1 (-> %2 :cmds first count)) 0 cmds)]
    (println "Usage: aws-util <subcommand> <options>")
    (println)
    (println "Subcommands")
    (doseq [{:keys [cmds desc]} cmds
            :when (seq cmds)
            :let [format-str (str "  %-" max-cmd-len "s  %s")
                  cmd (first cmds)]]
      (println (format format-str cmd desc)))))
