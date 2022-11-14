(ns hop-cli.bootstrap.profile
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [hop-cli.bootstrap.profile.registry :as profile.registry]
            [hop-cli.bootstrap.profile.template :as profile.template]))

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
  (doseq [{:keys [src dst]} files
          :let [src-path (build-bootstrap-resource-path src)
                dst-path (build-target-project-path settings dst)]]
    (if (fs/directory? src-path)
      (fs/copy-tree src-path dst-path {:replace-existing true})
      (fs/copy src-path dst-path {:replace-existing true}))))

(defn- write-dev-environment-variables-to-file!
  [settings environment-variables]
  (let [target-file (build-target-project-path settings ".env")]
    (->> (:dev environment-variables)
         (map #(format "%s=%s" (name (first %)) (second %)))
         sort
         (fs/write-lines target-file))))

(defn run-profiles!
  [settings]
  (let [profile-data (profile.registry/get-selected-profiles-data settings)
        updated-settings (assoc settings :project profile-data)
        project-path (build-target-project-path settings)]
    (copy-files! updated-settings (:files profile-data))
    (profile.template/render-profile-templates! updated-settings project-path)
    (write-dev-environment-variables-to-file! settings (:environment-variables profile-data))
    updated-settings))
