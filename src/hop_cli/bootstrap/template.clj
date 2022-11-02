(ns hop-cli.bootstrap.template
  (:require [babashka.fs :as fs]
            [cljstache.core :as cljstache]
            [clojure.string :as str]))

(def files
  [{:files [{:src "resources/bootstrap/project/core"
             :dst "new-project"}]
    :copy-if (constantly true)}
   {:files [{:src "resources/bootstrap/project/persistence"
             :dst "new-project"}]
    :copy-if :project/persistence?}])

(defn- filter-files-to-copy
  [settings files]
  (->> files
       (filter
        (fn [{:keys [copy-if]}]
          (copy-if settings)))
       (mapcat :files)))

(defn- copy-files!
  [settings]
  (let [files-to-copy (filter-files-to-copy settings files)]
    (doseq [{:keys [src dst]} files-to-copy]
      (fs/copy-tree src dst {:replace-existing true}))))

(defn- settings->mustache-data
  [settings]
  (reduce-kv
   (fn [m k v]
     (let [ns-keys (str/split (namespace k) #"\.")
           all-keys (conj ns-keys (name k))
           keywords (map keyword all-keys)]
       (assoc-in m keywords v)))
   {}
   settings))

(defn- render-file-name!
  [settings path]
  (let [current-name (fs/file-name path)
        mustache-data (settings->mustache-data settings)
        new-name (cljstache/render current-name mustache-data)]
    (when (not= current-name new-name)
      (let [new-path (.resolveSibling path new-name)]
        (fs/move path new-path)))))

(defn- render-file-content!
  [settings path]
  (let [file (fs/file path)
        template (slurp file)
        mustache-data (settings->mustache-data settings)
        rendered-content (cljstache/render template mustache-data)]
    (when (not= template rendered-content)
      (spit file rendered-content))))

(defn- render-templates!
  [settings file-path]
  (fs/walk-file-tree
   file-path
   {:visit-file
    (fn [path _]
      (render-file-content! settings path)
      (render-file-name! settings path)
      :continue)
    :post-visit-dir
    (fn [path _]
      (render-file-name! settings path)
      :continue)}))

(defn foo
  [settings]
  (copy-files! settings)
  (render-templates! settings "new-project"))
