(ns hop-cli.bootstrap.util
  (:require [clojure.string :as str]))

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
  [settings ks]
  (cond
    (keyword? ks)
    (get-settings-value settings (settings-kw->settings-path ks))

    (some #{:?} ks)
    (get-in settings
            (reduce
             (fn [acc k]
               (if (= :? k)
                 (conj acc (get-in settings (conj acc :value)))
                 (conj acc k)))
             []
             ks))
    :else
    (get-in settings ks)))

(defn build-profile-docker-files-to-copy
  [docker-compose-files profile-root-path extra-docker-files]
  (let [compose-files
        (->> docker-compose-files
             (vals)
             (apply concat)
             (map (fn [file] {:src (str profile-root-path file)})))]
    (if (and (seq compose-files) (seq extra-docker-files))
      (concat compose-files extra-docker-files)
      [])))
