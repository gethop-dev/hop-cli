;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.template
  (:require [babashka.fs :as fs]
            [cljfmt.core :as cljfmt]
            [clojure.pprint :refer [pprint]]
            [clojure.set :as set]
            [clojure.string :as str]
            [hop-cli.bootstrap.util :as bp.util]
            [hop-cli.util :as util]
            [hop-cli.util.file :as util.file]
            [pogonos.core :as pg]
            [zprint.core :as zprint]))

(def ^:const zprint-config
  {:width 90, :map {:comma? false}})

(def ^:const cljfmt-config
  {:sort-ns-references? true})

(defn- build-template-lambdas
  [settings render-fn]
  {:to-snake-case
   (fn [text]
     (str/replace (render-fn text) #"-" "_"))
   :resolve-choices
   (fn [text]
     (let [path (map keyword (str/split text #"\."))]
       (bp.util/get-settings-value settings path)))})

(defn- kv->formatted-string
  [[k v]]
  (str k
       "\n"
       (-> (with-out-str (pprint v))
           (str/trim-newline)
           (str/replace #"," ""))))

(defn- map->formatted-strings
  [m]
  (->> m
       (map kv->formatted-string)
       (interpose "\n\n")))

(defn- sequential-coll->formatted-strings
  [coll]
  (->> coll
       (map (fn [v]
              (if (string? v)
                v
                (str/trim-newline (with-out-str (pprint v))))))
       (interpose "\n")))

(defn- coll->formatted-strings
  [coll]
  (if (map? coll)
    (map->formatted-strings coll)
    (sequential-coll->formatted-strings coll)))

(defn- coll->escaped-string
  [coll]
  (->> coll
       (map #(format "\"%s\"" %1))
       (interpose " ")))

(defn- coll->docker-compose-environment-yaml-list
  [coll]
  (if (seq coll)
    (->> coll
         (map #(format "      %s:" %))
         (cons "    environment:")
         (interpose "\n"))
    []))

(defn- settings->mustache-data
  [settings]
  (-> settings
      (update-in [:project :config-edn]
                 util/update-map-vals coll->formatted-strings {:recursive? false})
      (update-in [:project :load-frontend-app]
                 (fn [load-frontend-app]
                   (into {}
                         (map (fn [[k v]]
                                [k (util/update-map-vals v coll->formatted-strings {:recursive? false})]))
                         load-frontend-app)))
      (update-in [:project :docker-compose]
                 util/update-map-vals #(str/join ":" %))
      (update-in [:project :deploy-files] coll->escaped-string)
      (update-in [:project :extra-app-docker-compose-environment-variables]
                 coll->docker-compose-environment-yaml-list)))

(defn- mustache-template-renderer*
  [mustache-data]
  (fn [content]
    (pg/render-string content mustache-data)))

(defn- mustache-template-renderer
  [settings]
  (let [mustache-data (settings->mustache-data settings)
        lambdas (build-template-lambdas settings (mustache-template-renderer* mustache-data))]
    (mustache-template-renderer* (assoc mustache-data :lambdas lambdas))))

(defn- format-file-content
  [path content]
  (cond
    (get #{"project.clj"} (fs/file-name path))
    (zprint/zprint-file-str content (str path) zprint-config)

    (get #{"edn" "clj" "cljs" "cljc"} (fs/extension path))
    (cljfmt/reformat-string content cljfmt-config)

    :else
    content))

(def file-exts-to-render
  #{"edn"
    "clj"
    "cljs"
    "cljc"
    "bb"
    "json"
    "sh"
    "yaml"
    "yml"
    "sql"
    "html"
    "service"})

(def file-names-to-render
  #{"Dockerfile"
    ".bashrc"})

(def file-exts-to-make-executable
  #{"sh"
    "bb"})

(defn render-profile-templates!
  [settings project-path]
  (let [renderer (mustache-template-renderer settings)
        files-to-render (set/union file-exts-to-render file-names-to-render)]
    (fs/walk-file-tree
     project-path
     {:visit-file
      (fn [path _]
        (let [new-path (or
                        (when (get files-to-render (fs/extension path))
                          (let [update-file-fn (fn [file-content]
                                                 (->> file-content
                                                      (renderer)
                                                      (format-file-content path)))]
                            (util.file/update-file-content! path update-file-fn)
                            (util.file/update-file-name! path renderer)
                            path))
                        path)]
          (when (get file-exts-to-make-executable (fs/extension new-path))
            (fs/set-posix-file-permissions new-path "rwxr-xr-x")))
        :continue)
      :post-visit-dir
      (fn [path _]
        (util.file/update-file-name! path renderer)
        :continue)})))
