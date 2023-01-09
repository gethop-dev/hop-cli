(ns editor
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [settings :as settings]
            [sidebar :as sidebar]))

(defn- build-docstring
  [{:keys [docstring]}]
  (when docstring
    (vec (concat [(first docstring) {:class "form__docstring"}] (rest docstring)))))

(defn input
  [node _opts conf]
  (let [{:keys [path value read-only? pattern]} node
        id (str/join "-" path)]
    [:div.form__field
     {:id (settings/build-node-id path)}
     [:label
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
  [node _opts conf]
  (let [{:keys [path value choices]} node
        {:keys [label-class]} conf
        id (str/join "-" path)]
    [:div.form__field
     [:label
      {:for id
       :class (when label-class
                label-class)
       :title (name (:name node))}
      (or (:tag node) (name (:name node)))]
     [build-docstring node]
     (when-not (= (:name node) :profiles)
       [:select
        (merge
         {:id id
          :value value
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
   {:keys [on-change-fn label-class hide-choices?]}
   config]
  (let [id (str/join "-" path)]
    [:div.form__field
     [:label
      {:for id
       :class (when label-class
                label-class)
       :title (name (:name node))}
      (or (:tag node) (name (:name node)))]
     [build-docstring node]
     (when-not hide-choices?
       [:div
        (for [choice choices
              :let [child-id (str "checkbox-" (settings/build-node-id (:path choice)))]]
          ^{:key (:name choice)}
          [:div
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

(defmethod form-component :plain-group
  [node opts]
  [:div.form__field.plain-group
   {:id (settings/build-node-id (:path node))}
   [:span.form__title
    {:title (name (:name node))}
    (or (:tag node) (name (:name node)))]
   [build-docstring node]
   (if-not (seq (:value node))
     [:span "No available configuration options."]
     (for [child (:value node)]
       ^{:key (:name child)}
       (form-component child opts)))])

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
                    :checked (:value node)}])

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
                 (str/starts-with? reference ":cloud-provider.")
                 (str
                  (second (re-find #":cloud-provider.([^.]+)." reference))
                  " cloud provider")
                 (str/starts-with? reference ":project.profiles.")
                 (str
                  (second (re-find #":project.profiles.([^.]+)." reference))
                  " profile"))]
    (if origin
      (str "Obtained from " origin)
      (str "Obtained automatically"))))

(defmethod form-component :ref
  [node opts]
  (let [reference (str (:value node))
        message (reference->message reference)]
    [input node opts {:placeholder message
                      :value ""
                      :title reference
                      :disabled true}]))

(defmethod form-component :single-choice-group
  [node opts]
  (let [selected-choice (settings/get-selected-single-choice node)]
    [:div.single-choice-group
     {:id (settings/build-node-id (:path node))}
     [select node opts {:label-class "form__title"}]
     (form-component selected-choice opts)]))

(defmethod form-component :multiple-choice-group
  [node opts]
  (let [selected-choices (settings/get-selected-multiple-choices node)]
    [:div.multiple-choice-group
     {:id (settings/build-node-id (:path node))}
     [checkbox-group node
      (merge opts {:hide-choices? (= :profiles (:name node))
                   :label-class "form__title"})
      {}]
     (for [choice selected-choices]
       ^{:key (:name choice)}
       (form-component choice opts))]))

(defmethod form-component :default
  [node _]
  [:span (:name node)])

(defn main
  []
  (let [settings (rf/subscribe [::settings/settings])]
    (fn []
      (when (seq @settings)
        [:div.settings-editor__main
         [sidebar/main @settings]
         [:form.settings-editor__form
          {:id "settings-editor-form"}
          (for [node @settings]
            ^{:key (:name node)}
            (form-component node {}))]]))))
