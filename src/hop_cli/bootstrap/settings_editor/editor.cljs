;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns editor
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [settings :as settings]
            [sidebar :as sidebar]
            [toolbar :as toolbar]
            [view :as view]))

(defn- build-docstring
  [{:keys [docstring]}]
  (when docstring
    (vec (concat [(first docstring) {:class "form__docstring"}] (rest docstring)))))

(defn- collapse-btn
  [collapsed?]
  [:button.btn.btn--flat.form__collapse-btn
   {:class (when @collapsed? "form__collapse-btn--collapsed")}
   [:img.form__collapse-btn-icon
    {:src "img/left-arrow.svg"
     :on-click
     (fn [e]
       (.preventDefault e)
       (swap! collapsed? not))}]])

(defn input
  [node _opts conf]
  (let [{:keys [path value read-only? pattern]} node
        id (str/join "-" path)]
    [:div.form__field
     {:id (settings/build-node-id path)}
     [:label.form__input-label
      {:for id
       :title (name (:name node))}
      (or (:tag node) (name (:name node)))]
     [build-docstring node]
     [:input.form__input
      (cond->
       {:id id
        :required true
        :disabled read-only?
        :value (str value)
        :on-change (fn [e]
                     (let [value (case (:type node)
                                   :keyword (keyword (.. e -target -value))
                                   :integer (js/parseInt (.. e -target -value))
                                   :boolean (boolean (.. e -target -checked))
                                   (.. e -target -value))]
                       (rf/dispatch [::settings/update-settings-value path value])))}
        pattern
        (assoc :pattern pattern)
        conf
        (merge conf))]]))

(defn select
  [node opts conf]
  (let [{:keys [path value choices]} node
        {:keys [collapsed-state]} opts
        {:keys [label-class]} conf
        id (str/join "-" path)]
    [:div.form__field
     [:div.form__group-header
      [:div.form__title-container
       [:label
        {:for id
         :class (when label-class
                  label-class)
         :title (name (:name node))}
        (or (:tag node) (name (:name node)))]
       [build-docstring node]]
      (when collapsed-state
        [collapse-btn collapsed-state])]
     (when-not (= (:name node) :profiles)
       [:select.form__selector
        (merge
         {:id id
          :value value
          :class (when (and collapsed-state @collapsed-state) "collapsed")
          :on-change (fn [e]
                       (let [value (if (:multiple conf)
                                     (mapv #(keyword (.-value %))
                                           (.. e -target -selectedOptions))
                                     (keyword (.. e -target -value)))]
                         (rf/dispatch [::settings/update-settings-value path value])))}
         conf)
        (for [choice choices]
          ^{:key (:name choice)}
          [:option
           {:value (:name choice)}
           (or (:tag choice) (name (:name choice)))])])]))

(defn checkbox-group
  [{:keys [path value choices] :as node}
   {:keys [on-change-fn label-class
           hide-choices? hide-label? collapsed-state
           field-class choices-group-class choice-class]}
   config]
  (let [id (str/join "-" path)]
    [:div.form__field
     {:class field-class}
     (when-not hide-label?
       [:div.form__group-header
        [:div.form__title-container
         [:label
          {:for id
           :class (when label-class
                    label-class)
           :title (name (:name node))}
          (or (:tag node) (name (:name node)))]
         [build-docstring node]]
        (when collapsed-state
          [collapse-btn collapsed-state])])
     (when-not hide-choices?
       [:div
        {:class [choices-group-class
                 (when (and collapsed-state @collapsed-state) "collapsed")]}
        (for [choice choices
              :let [child-id (str "checkbox-" (settings/build-node-id (:path choice)))]]
          ^{:key (:name choice)}
          [:div
           {:class choice-class}
           [:input
            (merge
             {:id child-id
              :type "checkbox"
              :checked (boolean (get (set value) (:name choice)))
              :on-change (if (fn? on-change-fn)
                           (partial on-change-fn choice path)
                           (fn [_]
                             (let [new-value (settings/toggle-value value (:name choice))]
                               (rf/dispatch [::settings/update-settings-value path new-value]))))}
             config)]
           [:label
            {:for child-id}
            (or (:tag choice) (name (:name choice)))]])])]))

(defmulti form-component
  (fn [{:keys [type]} _opts]
    type))

(defonce intersection-observer
  (js/IntersectionObserver.
   (fn [entries]
     (let [ids-to-add
           (->> entries
                (filter #(.-isIntersecting %))
                (mapv #(.. % -target -id)))
           ids-to-remove
           (->> entries
                (remove #(.-isIntersecting %))
                (mapv #(.. % -target -id)))]
       (rf/dispatch [::settings/update-visible-settings-node-ids ids-to-add ids-to-remove])))))

(defn- navigation-wrapper
  [node render-fn]
  (r/create-class
   {:component-did-mount
    (fn [_this]
      (let [element-id (settings/build-node-id (:path node))]
        (when-let [element (js/document.getElementById element-id)]
          (.observe intersection-observer element))))
    :component-will-unmount
    (fn [_this]
      (let [element-id (settings/build-node-id (:path node))]
        (when-let [element (js/document.getElementById element-id)]
          (.unobserve intersection-observer element))))
    :reagent-render render-fn}))

(defn- plain-group
  [node _opts]
  (let [collapsed? (r/atom false)]
    (navigation-wrapper
     node
     (fn [node opts]
       [:div.form__field.plain-group
        {:id (settings/build-node-id (:path node))}
        [:div.form__group-header
         [:div.form__title-container
          [:span.form__title
           {:title (name (:name node))}
           (or (:tag node) (name (:name node)))]
          [build-docstring node]]
         [collapse-btn collapsed?]]
        [:div.plain-group__children
         {:class (when @collapsed? "collapsed")}
         (if-not (seq (:value node))
           [:span
            "No available configuration options."]
           (for [child (:value node)]
             ^{:key (:name child)}
             (form-component child opts)))]]))))

(defmethod form-component :plain-group
  [node opts]
  [plain-group node opts])

(defmethod form-component :string
  [node opts]
  [input node opts {:type "text"}])

(defmethod form-component :keyword
  [node opts]
  [input node opts {:type "text"}])

(defmethod form-component :url
  [node opts]
  [input node opts {:type "url"}])

(defmethod form-component :email
  [node opts]
  [input node opts {:type "email"}])

(defmethod form-component :integer
  [node opts]
  [input node opts {:type "number"}])

(defmethod form-component :boolean
  [node opts]
  [input node opts {:type "checkbox"
                    :checked (:value node)
                    :class "form__input-checkbox"}])

(defmethod form-component :password
  [node opts]
  [input node opts {:type "password"}])

(defmethod form-component :auto-gen-password
  [node opts]
  [input node opts {:placeholder "Auto-generated"
                    :value ""
                    :title (str (:value node))
                    :disabled true}])

(defn reference->message
  [reference]
  (let [origin (cond
                 (str/starts-with? reference ":deployment-target.")
                 (str
                  (second (re-find #":deployment-target.([^.]+)." reference))
                  " deployment target")
                 (str/starts-with? reference ":project.profiles.")
                 (str
                  (second (re-find #":project.profiles.([^.]+)." reference))
                  " profile"))]
    (if origin
      (str "Obtained from " origin)
      "Obtained automatically")))

(defmethod form-component :ref
  [node opts]
  (let [reference (str (:value node))
        message (reference->message reference)
        settings (:settings opts)
        node-name-path (settings/get-path-from-ref-node node)
        invalid-selected-ref? (not (:success? (settings/lookup-ref settings node-name-path)))]
    [input node opts {:placeholder message
                      :value ""
                      :title reference
                      :disabled true
                      :class (when invalid-selected-ref?
                               "form__input--invalid")}]))

(defn- single-choice-group
  [node _opts]
  (let [collapsed? (r/atom false)]
    (navigation-wrapper
     node
     (fn [node opts]
       (let [selected-choice (settings/get-selected-single-choice node)]
         [:div.single-choice-group
          {:id (settings/build-node-id (:path node))}
          [select node
           (assoc opts :collapsed-state collapsed?)
           {:label-class "form__title"}]
          [:div.single-choice-group__choice-container
           {:class (when @collapsed? "collapsed")}
           (form-component selected-choice opts)]])))))

(defmethod form-component :single-choice-group
  [node opts]
  [single-choice-group node opts])

(defn- multiple-choice-group
  [node _opts]
  (let [collapsed? (r/atom false)]
    (navigation-wrapper
     node
     (fn [node opts]
       (let [selected-choices (settings/get-selected-multiple-choices node)]
         [:div.multiple-choice-group
          {:id (settings/build-node-id (:path node))}
          [checkbox-group node
           (merge opts {:collapsed-state collapsed?
                        :hide-choices? (= :profiles (:name node))
                        :label-class "form__title"})
           {}]
          [:div.multiple-choice-group__choices-container
           {:class (when @collapsed? "collapsed")}
           (for [choice selected-choices]
             ^{:key (:name choice)}
             (form-component choice opts))]])))))

(defmethod form-component :multiple-choice-group
  [node opts]
  [multiple-choice-group node opts])

(defmethod form-component :default
  [node _]
  [:span (:name node)])

(defn main
  []
  (let [settings (rf/subscribe [::settings/settings])]
    (fn []
      (when (seq @settings)
        [:div.editor
         [:div.editor__header
          [:button.btn.btn--flat.editor__back-btn
           {:on-click
            (fn [_]
              (rf/dispatch [::view/set-active-view :profile-picker]))}
           [:img.editor__btn-icon
            {:src "img/left-arrow.svg"}]
           "Edit selected profiles"]]
         [toolbar/main
          {:active-view :editor
           :title "Settings editor"
           :subtitle "HOP CLI Settings Editor"}]
         [sidebar/main @settings]
         [:form.editor__form
          {:id "settings-editor-form"}
          (doall
           (for [node (:value @settings)]
             ^{:key (:name node)}
             (form-component node {:settings @settings})))]]))))
