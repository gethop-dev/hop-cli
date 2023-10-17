;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.settings-patcher
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [hop-cli.bootstrap.util :as bp.util]
            [hop-cli.util :as util]
            [malli.core :as m]))

(defn- add-root-settings-node
  [settings]
  (if-not (vector? settings)
    settings
    {:name :settings
     :type :root
     :version "1.0"
     :value settings}))

(defn- cloud-provider->deployment-target
  [node]
  (if-not (map? node)
    node
    (case (:type node)
      :ref
      (update node :value (fn [ref-key]
                            (-> ref-key
                                (bp.util/settings-kw->settings-path)
                                (bp.util/swap-key-in-kw-path :cloud-provider :deployment-target)
                                (bp.util/settings-path->settings-kw))))

      (:plain-group :single-choice-group :multiple-choice-group)
      (update node :name (fn [node-name]
                           (if (= node-name :cloud-provider)
                             :deployment-target
                             node-name)))
      node)))

(def ^:private versions-schema
  [:map
   [:cli-version [:maybe string?]]
   [:settings-version [:maybe string?]]])

(def ^:private patches
  [{:patch-schema [:and
                   versions-schema
                   [:fn '(fn [{:keys [cli-version settings-version]}]
                           (and (or (zero? (compare cli-version "0.1.2"))
                                    (pos-int? (compare cli-version "0.1.2")))
                                (neg-int? (compare settings-version "1.0"))))]]
    :patch-fn cloud-provider->deployment-target}])

(defn- build-appliable-patches-fn
  [settings]
  (let [cli-version (util/get-version)
        settings-version (:version settings)
        appliable-patches-fns (reduce (fn [patch-fns {:keys [patch-schema patch-fn]}]
                                        (if (m/validate patch-schema {:cli-version cli-version
                                                                      :settings-version settings-version})
                                          (conj patch-fns patch-fn)
                                          patch-fns))
                                      []
                                      patches)]
    (when (seq appliable-patches-fns)
      ;; Beware that the order of execution is reversed with
      ;; `comp`. So it won't follow the same order as in `patches`
      ;; data structure.
      (apply comp appliable-patches-fns))))

(defn- version-str>version-vector
  [version-str]
  ;;NOTE the comparation treats all alpha versions as the same
  ;;version. It has to be improved.
  (->> (-> version-str
           (str/replace #"-.+" "")
           (str/split #"\."))
       (map #(Integer/parseInt %))
       vec))

(defn- compare-versions
  [version-a version-b]
  (compare
   (version-str>version-vector version-a)
   (version-str>version-vector version-b)))

(defn cli-and-settings-version-compatible?
  [settings]
  (let [cli-version (util/get-version)
        settings-version (:version settings)]
    ;; cli-version >= 0.1.2
    ;; and
    ;; settings-version <= 1.0 or settings-version >= 1.0
    (and (or (zero? (compare-versions cli-version "0.1.2"))
             (pos-int? (compare-versions cli-version "0.1.2")))
         (or (zero? (compare-versions settings-version "1.0"))
             ;; For a future scenario where we have a higher
             ;; incompatible version of settings.edn.
             (pos-int? (compare-versions settings-version "1.0"))
             (neg-int? (compare-versions settings-version "1.0"))))))

(defn apply-patches
  [settings]
  (let [appliable-patches-fn (build-appliable-patches-fn settings)]
    (-> (if (fn? appliable-patches-fn)
          (walk/prewalk appliable-patches-fn settings)
          settings)
        ;; Special patch that adds a root node to settings if it does
        ;; not have it.
        add-root-settings-node)))
