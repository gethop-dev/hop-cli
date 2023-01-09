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
    {:on-click #(scroll-to-path (:path node))}
    (:name node)]
   (for [child-node (:value node)
         :when (settings/group? child-node)]
     (sidebar-element child-node opts))])

(defmethod sidebar-element :single-choice-group
  [node opts]
  (let [child-node (settings/get-selected-single-choice node)]
    [:div.sidebar-element
     [:span.sidebar-element__title
      {:on-click #(scroll-to-path (:path node))}
      (:name node)]
     (sidebar-element child-node opts)]))

(defmethod sidebar-element :multiple-choice-group
  [node opts]
  (let [children (settings/get-selected-multiple-choices node)]
    [:div.sidebar-element
     [:span.sidebar-element__title
      {:on-click #(scroll-to-path (:path node))}
      (:name node)]
     (for [child-node children]
       (sidebar-element child-node opts))]))

(defmethod sidebar-element :default
  [_ _]
  [:span "Default"])

(defn main
  [settings]
  [:div.settings-editor__sidebar
   (for [node settings]
     (sidebar-element node {}))])
