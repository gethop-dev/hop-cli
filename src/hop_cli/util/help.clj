(ns hop-cli.util.help)

(defn print-help
  ([cmds]
   (print-help cmds ""))
  ([cmds main-cmd]
   (let [max-cmd-len (reduce #(max %1 (-> %2 :cmds first count)) 0 cmds)]
     (println (format "Usage: %s <subcommand> <options>" main-cmd))
     (println)
     (println "Subcommands")
     (doseq [{:keys [cmds desc]} cmds
             :when (seq cmds)
             :let [format-str (str "  %-" max-cmd-len "s  %s")
                   cmd (first cmds)]]
       (println (format format-str cmd desc))))))
