(ns hop-cli.bootstrap.settings
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:const ^:private default-settings-file-path
  "bootstrap/settings.edn")

(def ^:const ^:private file-name-count-sep-re
  #"-[0-9]+")

(defn- strip-file-name-count-sep
  "Returns the file name without the count separator.
  E.g.:
    - settings-1.edn -> settings.edn
    - settings-1 -> settings
    - settings.old-1.edn -> settings.old.edn"
  [file]
  (->> (str/split (fs/file-name file) file-name-count-sep-re)
       (reduce str)))

(defn- get-existing-files-count
  "Given a directory `dst-dir` and a file name `dst-file-name`, counts
  how many files exists in the directory with the given file name."
  [dst-dir dst-file-name]
  (->> dst-dir
       fs/list-dir
       (filter #(= (strip-file-name-count-sep %) dst-file-name))
       count))

(defn- get-new-dst-file
  [dst-dir dst-file-name]
  (let [file-count (get-existing-files-count dst-dir dst-file-name)
        [name ext] (fs/split-ext dst-file-name)
        dst-file-name (if (zero? file-count)
                        dst-file-name
                        (format "%s-%d%s" name file-count (if ext (str "." ext) "")))]
    (fs/file (str dst-dir fs/file-separator dst-file-name))))

(defn- copy*
  [src-file dst-file]
  (with-open [is (io/input-stream (io/resource src-file))]
    (if (fs/exists? dst-file)
      (io/copy is (get-new-dst-file (fs/parent dst-file) (fs/file-name dst-file)))
      (io/copy is dst-file))))

(defn copy
  [dst]
  (let [dst-file (fs/file (fs/canonicalize dst))]
    (cond
      (fs/directory? dst-file)
      {:success? false
       :reason :must-be-a-file}

      (not (fs/exists? (fs/parent dst-file)))
      {:success? false
       :reason :file-parent-directory-does-not-exist}

      :else
      (let [src-file default-settings-file-path]
        (copy* src-file dst-file)
        {:success? true}))))
