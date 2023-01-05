(ns editor
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [settings :as settings]
            [sidebar :as sidebar]))

(defn input
  [node opts conf]
  (let [{:keys [value read-only?]} node
        {:keys [path]} opts
        id (str/join "-" path)]
    [:div
     {:id (settings/build-node-id (:path opts))}
     [:label
      {:for id}
      (name (:name node))]
     [:br]
     [:input.form__input
      (merge
       {:id id
        :disabled read-only?
        :value (str value)
        :on-change (fn [e]
                     (let [value (case (:type node)
                                   :keyword (keyword (.. e -target -value))
                                   :integer (js/parseInt (.. e -target -value))
                                   :boolean (boolean (.. e -target -checked))
                                   (.. e -target -value))]
                       (rf/dispatch [::settings/update-settings-value path value])))}
       conf)]]))

(defn select
  [node opts conf]
  (let [{:keys [value choices]} node
        {:keys [path]} opts
        {:keys [label-class]} conf
        id (str/join "-" path)]
    [:div
     [:label
      {:for id
       :class (when label-class
                label-class)}
      (name (:name node))]
     [:br]
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
           (name (:name choice))])])]))

(defn checkbox-group
  [{:keys [value choices] :as node} {:keys [path]} {:keys [label-class hide-choices?]}]
  (let [id (str/join "-" path)]
    [:div
     [:label
      {:for id
       :class (when label-class
                label-class)}
      (name (:name node))]
     [:br]
     (when-not hide-choices?
       [:div
        (for [choice choices
              :let [child-path (conj path :choices (:name choice))
                    child-id (str "checkbox-" (settings/build-node-id child-path))]]
          ^{:key (:name choice)}
          [:div
           [:input
            {:id child-id
             :type "checkbox"
             :checked (boolean (get (set value) (:name choice)))
             :on-change
             (fn [_]
               (let [new-value (settings/toggle-value value (:name choice))]
                 (rf/dispatch [::settings/update-settings-value path new-value])))}]
           [:label
            {:for child-id}
            (:name choice)]])])]))

(defmulti form-component
  (fn [{:keys [type]} _opts]
    type))

(defmethod form-component :plain-group
  [node opts]
  (let [initial-path (:path opts)]
    [:div.plain-group
     {:id (settings/build-node-id (:path opts))}
     [:span.form__title (name (:name node))]
     (for [[index child] (keep-indexed vector (:value node))
           :let [path (conj initial-path :value index)]]
       ^{:key (:name child)}
       (form-component child (assoc opts :path path)))]))

(defmethod form-component :string
  [node opts]
  [input node opts {}])

(defmethod form-component :keyword
  [node opts]
  [input node opts {}])

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
  (let [[index selected-choice] (settings/get-selected-single-choice node)]
    [:div.single-choice-group
     {:id (settings/build-node-id (:path opts))}
     [select node opts {:label-class "form__title"}]
     (form-component selected-choice (update opts :path conj :choices index))]))

(defmethod form-component :multiple-choice-group
  [node opts]
  (let [selected-choices (settings/get-selected-multiple-choices node)]
    [:div.multiple-choice-group
     {:id (settings/build-node-id (:path opts))}
     [checkbox-group node opts {:hide-choices? (= :profiles (:name node))
                                :label-class "form__title"}]
     (for [[index choice] selected-choices]
       ^{:key (:name choice)}
       (form-component choice (update opts :path conj :choices index)))]))

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
         [:div.settings-editor__form
          (for [[index node] (keep-indexed vector @settings)]
            ^{:key (:name node)}
            (form-component node {:path [index]}))]]))))
