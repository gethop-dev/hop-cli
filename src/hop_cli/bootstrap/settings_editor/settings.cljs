(ns settings
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [re-frame.core :as rf]))

(defn build-node-id
  [path]
  (str/join "_" path))

(defn group?
  [node]
  (get #{:plain-group :single-choice-group :multiple-choice-group}
       (:type node)))

(defn get-selected-single-choice
  [node]
  (some (fn [choice]
          (when (= (:name choice) (:value node))
            choice))
        (:choices node)))

(defn get-selected-multiple-choices
  [node]
  (filter (fn [choice]
            (when (get (set (:value node)) (:name choice))
              choice))
          (:choices node)))

(defn get-selected-children
  [node]
  (cond
    (vector? node)
    node

    (= (:type node) :plain-group)
    (:value node)

    (= (:type node) :single-choice-group)
    [(get-selected-single-choice node)]

    (= (:type node) :multiple-choice-group)
    (get-selected-multiple-choices node)

    :else []))

(defn get-children
  [node]
  (cond
    (vector? node)
    node

    (= (:type node) :plain-group)
    (:value node)

    (= (:type node) :single-choice-group)
    (:choices node)

    (= (:type node) :multiple-choice-group)
    (:choices node)

    :else []))

(defn find-child
  [children child-name]
  (some (fn [child-node]
          (when (= (:name child-node) child-name)
            child-node))
        children))

(defn get-node-path
  [settings node-name-path]
  (loop [settings settings
         node-name-path node-name-path
         path []]
    (if-not (seq node-name-path)
      path
      (let [next-node-name (first node-name-path)
            children-key (if (= (:type settings) :plain-group)
                           :value
                           :choices)
            children-nodes (if (vector? settings)
                             settings
                             (get settings children-key))
            [next-index next-node] (some (fn [[index node]]
                                           (when (= (:name node) next-node-name)
                                             [index node]))
                                         (keep-indexed vector children-nodes))]
        (when next-node
          (recur next-node
                 (rest node-name-path)
                 (if (vector? settings)
                   (conj path next-index)
                   (conj path children-key next-index))))))))

(defn toggle-value
  [old-values new-value]
  (if (get (set old-values) new-value)
    (vec (remove #{new-value} old-values))
    (conj old-values new-value)))

(defn- add-path-to-children
  [node children-key]
  (update node children-key
          #(vec (map-indexed (fn [idx child-node]
                               (assoc child-node
                                      :path (conj (vec (:path node)) children-key idx)
                                      :node-name-path (conj (vec (:node-name-path node)) (:name child-node))))
                             %))))

(defn add-paths
  [settings]
  (walk/prewalk (fn [node]
                  (if-not (map? node)
                    node
                    (case (:type node)
                      :root
                      (vec (map-indexed (fn [idx child-node]
                                          (assoc child-node
                                                 :path [idx]
                                                 :node-name-path [(:name child-node)]))
                                        (:value node)))

                      :plain-group
                      (add-path-to-children node :value)

                      (:single-choice-group :multiple-choice-group)
                      (add-path-to-children node :choices)

                      node)))
                {:type :root
                 :value settings}))

(defn remove-paths
  [settings]
  (walk/prewalk (fn [node]
                  (if (map? node)
                    (dissoc node :path :node-name-path)
                    node))
                settings))

(defn get-selected-refs
  [node]
  (cond
    (vector? node)
    (mapcat get-selected-refs node)

    (= (:type node) :ref)
    [node]

    (= (:type node) :single-choice-group)
    (get-selected-refs (settings/get-selected-single-choice node))

    (= (:type node) :multiple-choice-group)
    (mapcat get-selected-refs (settings/get-selected-multiple-choices node))

    (= (:type node) :plain-group)
    (mapcat get-selected-refs (:value node))

    :else []))

(defn lookup-ref
  [settings node-name-path]
  (loop [node settings
         node-name-path node-name-path]
    (if-not (seq node-name-path)
      {:success? true}
      (let [next-node-name (if (= (first node-name-path) :?)
                             (:value node)
                             (first node-name-path))
            children-nodes (settings/get-children node)
            selected-children-nodes (settings/get-selected-children node)]
        (if-not (settings/find-child children-nodes next-node-name)
          {:success? true}
          (if-let [next-node (settings/find-child selected-children-nodes next-node-name)]
            (recur next-node (rest node-name-path))
            {:success? false}))))))

(defn get-path-from-ref-node
  [{:keys [value]}]
  (->> (str/split (str (namespace value) "." (name value)) #"\.")
       (map keyword)))

(rf/reg-sub
 ::settings
 (fn [db _]
   (get db :settings)))

(rf/reg-event-db
 ::set-settings
 (fn [db [_ settings]]
   (assoc db :settings (add-paths settings))))

(rf/reg-event-db
 ::update-settings-value
 (fn [db [_ path value]]
   (assoc-in db (cons :settings (conj path :value)) value)))

(rf/reg-sub
 ::visible-settings-node-ids
 (fn [db _]
   (get db :visible-settings-node-ids)))

(rf/reg-event-fx
 ::update-visible-settings-node-ids
 (fn [{:keys [db]} [_ to-add to-remove]]
   (let [visible-ids (->> (:visible-settings-node-ids db)
                          (remove #(get (set to-remove) %))
                          (concat to-add))]
     {:db (assoc db :visible-settings-node-ids visible-ids)})))
