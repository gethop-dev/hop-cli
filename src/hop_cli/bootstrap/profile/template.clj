(ns hop-cli.bootstrap.profile.template
  (:require [babashka.fs :as fs]
            [cljfmt.core :as cljfmt]
            [cljstache.core :as cljstache]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [hop-cli.bootstrap.util :as bp.util]
            [hop-cli.util :as util]
            [hop-cli.util.file :as util.file]
            [zprint.core :as zprint]))

(def ^:const zprint-config
  {:width 90, :map {:comma? false}})

(def ^:const cljfmt-config
  {:sort-ns-references? true})

(defn- build-template-lambdas
  [settings]
  {:to-snake-case
   (fn [text]
     (fn [render-fn]
       (str/replace (render-fn text) #"-" "_")))
   :resolve-choices
   (fn [text]
     (let [path (map keyword (str/split text #"\."))]
       (bp.util/get-settings-value settings path)))})

(defn- kv->formatted-string
  [[k v]]
  (str k "\n" (str/replace (with-out-str (pprint v)) #"," "")))

(defn- map->formatted-string
  [m]
  (->> m
       (map kv->formatted-string)
       (interpose "\n\n")))

(defn- sequential-coll->formatted-string
  [coll]
  (->> coll
       (map #(if (string? %) % (with-out-str (pprint %))))
       (interpose "\n")))

(defn- coll->formatted-string
  [coll]
  (if (map? coll)
    (map->formatted-string coll)
    (sequential-coll->formatted-string coll)))

(defn- coll->escaped-string
  [coll]
  (->> coll
       (map #(format "\"%s\"" %1))
       (interpose " ")))

(defn- settings->mustache-data
  [settings]
  (-> settings
      (update-in [:project :config-edn]
                 util/update-map-vals coll->formatted-string {:recursive? false})
      (update-in [:project :load-frontend-app]
                 util/update-map-vals coll->formatted-string {:recursive? false})
      (update-in [:project :docker-compose]
                 util/update-map-vals #(str/join ":" %))
      (update-in [:project :deploy-files] coll->escaped-string)))

(defn- mustache-template-renderer
  [settings]
  (let [mustache-data (-> settings
                          (settings->mustache-data)
                          (assoc :lambdas (build-template-lambdas settings)))]
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

(defn render-profile-templates!
  [settings project-path]
  (prn project-path)
  (let [renderer (mustache-template-renderer settings)]
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
