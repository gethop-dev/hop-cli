(ns sidebar
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [settings :as settings]
            [view :as view]))

(defmulti sidebar-element
  (fn [{:keys [type]} _opts]
    type))

(defn- sidebar-element-wrapper
  [_node _opts _get-children-fn]
  (let [collapsed? (r/atom false)]
    (fn [node opts get-children-fn]
      (let [children (get-children-fn node opts)
            element-id (settings/build-node-id (:path node))]
        [:div.sidebar-element
         {:class [(:class opts)]
          :id (str "sidebar__" element-id)}
         [:div.sidebar-element__title-container
          (when (seq children)
            [:img.sidebar-element__collapse-btn
             {:src "img/left-arrow.svg"
              :on-click #(swap! collapsed? not)
              :class (when @collapsed?
                       "sidebar-element__collapse-btn--collapsed")}])
          [:span.sidebar-element__title
           {:on-click #(rf/dispatch [::view/scroll-to-element  element-id])
            :class (when (get (:visible-ids opts) element-id)
                     "sidebar-element-title--visible")}
           (:name node)]]
         (for [child-node children]
           (sidebar-element
            child-node
            (cond-> opts
              @collapsed?
              (assoc :class "collapsed"))))]))))

(defmethod sidebar-element :plain-group
  [node opts]
  [sidebar-element-wrapper
   node opts
   (fn [node _opts]
     (filter settings/group? (:value node)))])

(defmethod sidebar-element :single-choice-group
  [node opts]
  [sidebar-element-wrapper
   node opts
   (fn [node _opts]
     [(settings/get-selected-single-choice node)])])

(defmethod sidebar-element :multiple-choice-group
  [node opts]
  [sidebar-element-wrapper
   node opts
   (fn [node _opts]
     (settings/get-selected-multiple-choices node))])

(defmethod sidebar-element :default
  [_ _]
  [:span "Default"])

(defn main
  [_settings]
  (let [visible-ids (rf/subscribe [::settings/visible-settings-node-ids])]
    (fn [settings]
      (let [visible-ids (set @visible-ids)]
        [:div.sidebar
         (for [node (:value settings)]
           (sidebar-element node {:visible-ids visible-ids}))]))))
