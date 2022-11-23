(ns hop-cli.bootstrap.profile
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [hop-cli.bootstrap.profile.registry :as profile.registry]
            [hop-cli.bootstrap.profile.template :as profile.template]
            [hop-cli.bootstrap.settings-reader :as settings-reader]
            [hop-cli.bootstrap.util :as bp.util]
            [meta-merge.core :refer [meta-merge]]))

(defn- build-target-project-path
  ([settings]
   (build-target-project-path settings nil))
  ([settings subpath]
   (let [target-dir (bp.util/get-settings-value settings :project/target-dir)]
     (str (fs/normalize target-dir) fs/file-separator subpath))))

(defn- build-bootstrap-resource-path
  [subpath]
  (io/resource (str "bootstrap/profiles/" subpath)))

(defn- copy-files!
  [settings]
  (let [files-to-copy (bp.util/get-settings-value settings [:project :files])]
    (doseq [{:keys [src dst]} files-to-copy
            :let [src-path (build-bootstrap-resource-path src)
                  dst-path (build-target-project-path settings dst)]]
      (if (fs/directory? src-path)
        (fs/copy-tree src-path dst-path {:replace-existing true})
        (fs/copy src-path dst-path {:replace-existing true})))))

(defn- write-dev-environment-variables-to-file!
  [settings]
  (let [env-variables (bp.util/get-settings-value settings
                                                  [:project :environment-variables])
        target-file (build-target-project-path settings ".env")]
    (->> (:dev env-variables)
         (map #(format "%s=%s" (name (first %)) (second %)))
         sort
         (fs/write-lines target-file))))

(defn- execute-profiles
  [settings]
  (let [selected-profiles (bp.util/get-settings-value settings
                                                      [:project :profiles :value])
        selected-profile-set (set (cons :core selected-profiles))]
    (->> profile.registry/profiles
         (filterv #(get selected-profile-set (:kw %)))
         (reduce
          (fn [settings {:keys [kw exec-fn]}]
            (let [resolved-settings (settings-reader/resolve-refs settings [:project :profiles kw])
                  result (exec-fn resolved-settings)]
              (meta-merge
               resolved-settings
               {:project (-> result
                             (dissoc :outputs)
                             (assoc-in [:profiles kw] (:outputs result)))})))
          settings))))

(defn generate-project!
  [settings]
  (let [updated-settings (-> settings
                             (execute-profiles)
                             (settings-reader/resolve-refs [:project]))
        project-path (build-target-project-path settings)]
    (copy-files! updated-settings)
    (profile.template/render-profile-templates! updated-settings project-path)
    (write-dev-environment-variables-to-file! updated-settings)
    {:success? true :settings updated-settings}))
