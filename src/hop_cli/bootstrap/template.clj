(ns hop-cli.bootstrap.template
  (:require [babashka.fs :as fs]
            [cljfmt.core :as cljfmt]
            [cljstache.core :as cljstache]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [hop-cli.bootstrap.profile.bi.grafana :as profile.bi.grafana]
            [hop-cli.bootstrap.profile.core :as profile.core]
            [hop-cli.bootstrap.profile.persistence :as profile.persistence]
            [hop-cli.util :as util]
            [hop-cli.util.file :as util.file]
            [zprint.core :as zprint]))

(def ^:const zprint-config
  {:width 90, :map {:comma? false}})

(def ^:const cljfmt-config
  {:sort-ns-references? true})

(defn- filter-files-to-copy
  [settings files]
  (->> files
       (filter
        (fn [{:keys [copy-if]}]
          (if copy-if
            (copy-if settings)
            true)))
       (mapcat :files)))

(defn- copy-files!
  [settings files]
  (let [files-to-copy (filter-files-to-copy settings files)]
    (doseq [{:keys [src dst]} files-to-copy]
      (fs/copy-tree src dst {:replace-existing true}))))

(defn- kv->edn-formatted-string
  [[k v]]
  (str k "\n" (str/replace (with-out-str (pprint v)) #"," "")))

(defn map->edn-formatted-string
  [config]
  (->> config
       (map kv->edn-formatted-string)
       (interpose "\n\n")))

(defn- settings->mustache-data
  [settings]
  (-> settings
      (util/expand-ns-keywords)
      (update-in [:profiles :config-edn :base] map->edn-formatted-string)))

(defn- mustache-template-renderer
  [settings]
  (let [mustache-data (settings->mustache-data settings)]
    (fn [content]
      (cljstache/render content mustache-data))))

(defn- format-file-content
  [path content]
  (cond
    (get #{"project.clj"} (fs/file-name path))
    (zprint/zprint-file-str content (str path) zprint-config)

    (get #{"edn" "clj" "cljs" "cljc"} (fs/extension path))
    (cljfmt/reformat-string content cljfmt-config)

    :else
    content))

(defn- render-templates!
  [settings file-path]
  (let [renderer (mustache-template-renderer settings)]
    (fs/walk-file-tree
     file-path
     {:visit-file
      (fn [path _]
        (let [update-file-fn (fn [file-content]
                               (->> file-content
                                    (renderer)
                                    (format-file-content path)))]
          (util.file/update-file-content! path update-file-fn)
          (util.file/update-file-name! path renderer))
        :continue)
      :post-visit-dir
      (fn [path _]
        (util.file/update-file-name! path renderer)
        :continue)})))

(defn- merge-profile-key
  [k v1 v2]
  (cond
    (get #{:dependencies :files} k)
    (vec (concat v1 v2))

    (get #{:environment-variables} k)
    (merge-with merge v1 v2)

    :else
    (merge v1 v2)))

(defn- write-dev-environment-variables-to-file!
  [settings environment-variables]
  (let [target-file (str (:target-project-dir settings) "/.env")]
    (->> (:dev environment-variables)
         (map #(format "%s=%s" (name (first %)) (second %)))
         (fs/write-lines target-file))))

(defn foo
  [settings]
  (let [profiles [(profile.core/profile settings)
                  (profile.persistence/profile settings)
                  (profile.bi.grafana/profile settings)]
        profile-data (apply util/merge-with-key merge-profile-key profiles)]
    (copy-files! settings (:files profile-data))
    (render-templates! (assoc settings :profiles profile-data) (:target-project-dir settings))
    (write-dev-environment-variables-to-file! settings (:environment-variables profile-data))
    {:environment-variables (:environment-variables profile-data)}))
