(ns sidebar
  (:require [settings]))

(defn- scroll-to-path
  [path]
  (let [element-id (settings/build-node-id path)
        js-element (js/document.getElementById element-id)]
    (.scrollIntoView js-element #js {:behavior "smooth"})))

(defmulti sidebar-element
  (fn [{:keys [type]} _opts]
    type))

(defmethod sidebar-element :plain-group
  [node opts]
  [:div.sidebar-element
   [:span.sidebar-element__title
    {:on-click #(scroll-to-path (:path opts))}
    (:name node)]
   (for [[index child-node] (keep-indexed vector (:value node))
         :when (settings/group? child-node)]
     (sidebar-element child-node (update opts :path conj :value index)))])

(defmethod sidebar-element :single-choice-group
  [node opts]
  (let [[index child-node] (settings/get-selected-single-choice node)]
    [:div.sidebar-element
     [:span.sidebar-element__title
      {:on-click #(scroll-to-path (:path opts))}
      (:name node)]
     (sidebar-element child-node (update opts :path conj :choices index))]))

(defmethod sidebar-element :multiple-choice-group
  [node opts]
  (let [children (settings/get-selected-multiple-choices node)]
    [:div.sidebar-element
     [:span.sidebar-element__title
      {:on-click #(scroll-to-path (:path opts))}
      (:name node)]
     (for [[index child-node] children]
       (sidebar-element child-node (update opts :path conj :choices index)))]))

(defn main
  [settings]
  [:div.settings-editor__sidebar
   (for [[index node] (keep-indexed vector settings)]
     (sidebar-element node {:path [index]}))])
