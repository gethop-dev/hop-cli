;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.util
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [meta-merge.core :refer [meta-merge]]))

(defn get-env-type
  [environment]
  (case environment
    :dev :to-develop
    :test :to-deploy
    :prod :to-deploy))

(defn settings-kw->settings-path
  [kw]
  (if (simple-keyword? kw)
    [kw]
    (conj (mapv keyword (str/split (namespace kw) #"\.")) (keyword (name kw)))))

(defn get-settings-value
  ([settings ks]
   (get-settings-value settings ks nil))
  ([settings ks not-found]
   (cond
     (keyword? ks)
     (get-settings-value settings (settings-kw->settings-path ks) not-found)

     (some #{:?} ks)
     (get-in settings
             (reduce
              (fn [acc k]
                (if (= :? k)
                  (conj acc (get-in settings (conj acc :value) not-found))
                  (conj acc k)))
              []
              ks))
     :else
     (get-in settings ks not-found))))

(defn assoc-in-settings-value
  [settings ks v]
  (if (coll? ks)
    (assoc-in settings ks v)
    (assoc-in settings (settings-kw->settings-path ks) v)))

(defn merge-settings-value
  [settings ks v]
  (if (coll? ks)
    (update-in settings ks meta-merge v)
    (update-in settings (settings-kw->settings-path ks) meta-merge v)))

(defn build-profile-docker-files-to-copy
  [docker-compose-files profile-root-path extra-docker-files]
  (let [compose-files
        (->> docker-compose-files
             (vals)
             (apply concat)
             (into #{})
             (mapv (fn [file]
                     {:src (str profile-root-path file)})))]
    (if (and (seq compose-files) (seq extra-docker-files))
      (into compose-files extra-docker-files)
      compose-files)))

(defn build-target-project-path
  ([settings]
   (build-target-project-path settings nil))
  ([settings subpath]
   (let [target-dir (get-settings-value settings :project/target-dir)]
     (str (fs/normalize target-dir) fs/file-separator subpath))))

(defn env-var->string-env-var
  [[k v]]
  (let [quoted-val (-> v
                       (str/replace "\\" "\\\\")
                       (str/replace "'" "\\'"))]
    (format "%s='%s'" (name k) quoted-val)))

(def ^:const ^:private vars-file-preamble
  ["# WARNING! Make sure all the variable values defined in this file"
   "# are quoted using single quotes. And escape any single quote characters"
   "# that may be present in the variable values, by prefixing them with"
   "# with a backslash character. Which means that you should also quote"
   "# any backslash character present in the values, by doubling them."
   ""])

(defn write-variables-to-file!
  [env-vars-file vars-lines]
  (fs/write-lines env-vars-file vars-file-preamble)
  (fs/write-lines env-vars-file (sort vars-lines) {:append true}))

(defn write-environment-variables-to-file!
  ([settings environment]
   (write-environment-variables-to-file! settings
                                         environment
                                         (format ".env.%s" (name environment))))
  ([settings environment file-name]
   (let [env-variables (get-settings-value settings
                                           [:project :environment-variables])
         target-file (build-target-project-path settings file-name)
         env-vars-lines (->> (get env-variables environment)
                             (mapv env-var->string-env-var))]
     (write-variables-to-file! target-file env-vars-lines))))

(defn write-environments-env-vars-to-file!
  [settings]
  (->> (get-settings-value settings :project/environments)
       (remove #{:dev})
       (set)
       (mapv (partial write-environment-variables-to-file! settings))))

(defn swap-key-in-kw-path
  [kw-path old-key new-key]
  (map (fn [k]
         (if (= k old-key)
           new-key
           k))
       kw-path))

(defn settings-path->settings-kw
  [kw-path]
  (keyword (reduce (fn [ns-name kw]
                     (if-not (seq ns-name)
                       (name kw)
                       (str ns-name "." (name kw))))
                   ""
                   (butlast kw-path))
           (name (last kw-path))))
