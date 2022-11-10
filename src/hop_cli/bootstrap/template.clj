(ns hop-cli.bootstrap.template
  (:require [babashka.fs :as fs]
            [cljfmt.core :as cljfmt]
            [cljstache.core :as cljstache]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [hop-cli.bootstrap.profile.authentication.cognito :as profile.cognito]
            [hop-cli.bootstrap.profile.aws :as profile.aws]
            [hop-cli.bootstrap.profile.bi.grafana :as profile.bi.grafana]
            [hop-cli.bootstrap.profile.core :as profile.core]
            [hop-cli.bootstrap.profile.docker :as profile.docker]
            [hop-cli.bootstrap.profile.frontend :as profile.frontend]
            [hop-cli.bootstrap.profile.persistence.sql :as profile.persistence.sql]
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
            true)))))

(defn- build-target-project-path
  ([settings]
   (build-target-project-path settings nil))
  ([settings subpath]
   (str (fs/normalize (:target-project-dir settings)) fs/file-separator subpath)))

(defn- build-bootstrap-resource-path
  [subpath]
  (io/resource (str "bootstrap/profiles/" subpath)))

(defn- copy-files!
  [settings files]
  (let [files-to-copy (filter-files-to-copy settings files)]
    (doseq [{:keys [src dst]} files-to-copy
            :let [src-path (build-bootstrap-resource-path src)
                  dst-path (build-target-project-path settings dst)]]
      (fs/copy-tree src-path dst-path {:replace-existing true}))))

(defn- kv->formatted-string
  [[k v]]
  (str k "\n" (str/replace (with-out-str (pprint v)) #"," "")))

(defn map->formatted-string
  [m]
  (->> m
       (map kv->formatted-string)
       (interpose "\n\n")))

(defn- sequential-coll->formatted-string
  [coll]
  (->> coll
       (map #(if (string? %) % (with-out-str (pprint %))))
       (interpose "\n")))

(defn coll->formatted-string
  [coll]
  (if (map? coll)
    (map->formatted-string coll)
    (sequential-coll->formatted-string coll)))

(defn- settings->mustache-data
  [settings]
  (clojure.pprint/pprint (update-in settings [:profiles :load-frontend-app] util/update-map-vals coll->formatted-string {:recursive? false}))
  (-> settings
      (util/expand-ns-keywords)
      (update-in [:profiles :config-edn] util/update-map-vals coll->formatted-string {:recursive? false})
      (update-in [:profiles :load-frontend-app] util/update-map-vals coll->formatted-string {:recursive? false})))

(def ^:private lambdas
  {:to-snake-case (fn [text]
                    (fn [render-fn]
                      (str/replace (render-fn text) #"-" "_")))})

(defn- mustache-template-renderer
  [settings]
  (let [mustache-data (-> settings
                          (settings->mustache-data)
                          (assoc :lambdas lambdas))]
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
  [settings]
  (let [project-path (build-target-project-path settings)
        renderer (mustache-template-renderer settings)]
    (fs/walk-file-tree
     project-path
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

    (get #{:environment-variables :config-edn} k)
    (merge-with merge v1 v2)

    (get #{:load-frontend-app} k)
    (merge-with concat v1 v2)

    :else
    (merge v1 v2)))

(defn- write-dev-environment-variables-to-file!
  [settings environment-variables]
  (let [target-file (build-target-project-path settings ".env")]
    (->> (:dev environment-variables)
         (map #(format "%s=%s" (name (first %)) (second %)))
         sort
         (fs/write-lines target-file))))

(defn foo
  [settings]
  (let [profiles [(profile.core/profile settings)
                  (profile.frontend/profile settings)
                  (profile.persistence.sql/profile settings)
                  (profile.bi.grafana/profile settings)
                  (profile.cognito/profile settings)
                  (profile.docker/profile settings)
                  (profile.aws/profile settings)]
        profile-data (apply util/merge-with-key merge-profile-key profiles)]
    (copy-files! settings (:files profile-data))
    (render-templates! (assoc settings :profiles profile-data))
    (write-dev-environment-variables-to-file! settings (:environment-variables profile-data))
    {:environment-variables (:environment-variables profile-data)}))
