(ns hop-cli.util.error
  (:require [babashka.cli :as cli]))

(defn generic-error-handler
  [{:keys [msg spec]}]
  (println "An error has occurred:" msg)
  (println "Usage:")
  (println (cli/format-opts {:spec spec}))
  (System/exit 1))
