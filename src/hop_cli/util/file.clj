(ns hop-cli.util.file
  (:require [babashka.fs :as fs]))

(defn update-file-content!
  [path update-fn]
  (let [file (fs/file path)
        content (slurp file)
        updated-content (update-fn content)]
    (when (not= updated-content content)
      (spit file updated-content))))

(defn update-file-name!
  [path update-fn]
  (let [current-name (fs/file-name path)
        new-name (update-fn current-name)]
    (when (not= current-name new-name)
      (let [new-path (.resolveSibling path new-name)]
        (fs/move path new-path)))))
