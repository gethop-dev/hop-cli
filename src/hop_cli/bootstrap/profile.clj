;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [hop-cli.bootstrap.profile.registry :as registry]
            [hop-cli.bootstrap.profile.registry-loader :as profile.registry-loader]
            [hop-cli.bootstrap.profile.template :as profile.template]
            [hop-cli.bootstrap.settings-reader :as settings-reader]
            [hop-cli.bootstrap.util :as bp.util]
            [hop-cli.util.file :as util.file]
            [meta-merge.core :refer [meta-merge]]))

(defn- build-target-project-path
  ([settings]
   (build-target-project-path settings nil))
  ([settings subpath]
   (let [target-dir (bp.util/get-settings-value settings :project/target-dir)]
     (str (fs/normalize target-dir) fs/file-separator subpath))))

(defn- copy-files!*
  [settings bootstrap-path-prefix]
  (let [files-to-copy (bp.util/get-settings-value settings [:project :files])]
    (doseq [{:keys [src dst]} files-to-copy
            :let [src-path (fs/path bootstrap-path-prefix "profiles" src)
                  dst-path (build-target-project-path settings dst)]]
      (if (fs/directory? src-path)
        (fs/copy-tree src-path dst-path {:replace-existing true})
        (fs/copy src-path dst-path {:replace-existing true})))))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(defn- copy-files!
  [settings]
  (if-let [jar-file-path (util.file/get-jar-file-path)]
    (fs/with-temp-dir [temp-dir {}]
      (fs/unzip jar-file-path temp-dir)
      (copy-files!* settings (fs/path temp-dir "bootstrap")))
    (copy-files!* settings (io/resource "bootstrap"))))

(defn- write-dev-environment-variables-to-file!
  [settings]
  (let [env-variables (bp.util/get-settings-value settings
                                                  [:project :environment-variables])
        target-file (build-target-project-path settings ".env")]
    (->> (:dev env-variables)
         (map #(format "%s=%s" (name (first %)) (second %)))
         sort
         (fs/write-lines target-file))))

(defn get-selected-profiles
  [settings]
  (let [selected-profiles (bp.util/get-settings-value settings [:project :profiles :value])
        selected-profile-set (set selected-profiles)]
    (->> profile.registry-loader/profile-list
         (filterv #(get selected-profile-set %)))))

(defn- execute-profile-hook
  [settings profiles hook-fn]
  (reduce
   (fn [settings profile-kw]
     (let [resolved-settings
           (settings-reader/resolve-refs settings [:project :profiles profile-kw])
           result (hook-fn profile-kw resolved-settings)]
       (meta-merge
        resolved-settings
        {:project (-> result
                      (dissoc :outputs)
                      (assoc-in [:profiles profile-kw] (:outputs result)))})))
   settings
   profiles))

(defn- generate-project!
  [settings]
  (let [project-path (build-target-project-path settings)]
    (copy-files! settings)
    (profile.template/render-profile-templates! settings project-path)
    (write-dev-environment-variables-to-file! settings)
    settings))

(defn execute-profiles!
  [settings]
  (let [profiles (get-selected-profiles settings)
        environments (bp.util/get-settings-value settings :project/environments)
        updated-settings
        (-> settings
            (execute-profile-hook profiles registry/pre-render-hook)
            (settings-reader/resolve-refs [:project])
            (cond-> (get (set environments) :dev) (generate-project!))
            (execute-profile-hook profiles registry/post-render-hook))]
    {:success? true :settings updated-settings}))
