(ns main
  (:require [ajax.core :refer [GET]]
            [cljs.pprint :as pprint]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [file]
            [re-frame.core :as rf]
            [reagent.dom :as rdom]
            [settings :as settings]
            [sidebar]))

(rf/reg-fx
 :do-get-request
 (fn [{:keys [uri handler-evt]}]
   (GET uri {:handler (fn [response]
                        (rf/dispatch (conj handler-evt response)))})))

(rf/reg-event-fx
 ::default-settings-loaded
 (fn [_ [_ response]]
   {:fx [[:dispatch [::settings/set-settings (edn/read-string response)]]]}))

(rf/reg-event-fx
 ::settings-file-loaded
 (fn [_ [_ {:keys [content] :as _file}]]
   {:fx [[:dispatch [::settings/set-settings (edn/read-string content)]]]}))

(rf/reg-event-fx
 ::save-settings-to-file
 (fn [{:keys [db]} _]
   (let [settings-data (:settings db)
         pprinted-settings (with-out-str (pprint/pprint settings-data))
         file-name "settings.edn"
         file-type "application/edn"]
     {:fx [[:write-to-file {:file-name file-name
                            :file-type file-type
                            :data pprinted-settings}]]})))

(rf/reg-event-fx
 ::read-settings-from-file
 (fn [_ [_ js-file]]
   {:fx [[:read-from-file {:js-file js-file
                           :on-read-evt [::settings-file-loaded]}]]}))

(rf/reg-event-fx
 ::load-default-settings
 (fn [_]
   {:do-get-request {:uri "/settings.edn"
                     :handler-evt [::default-settings-loaded]}}))

(rf/dispatch-sync [::load-default-settings])

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

(defn input
  [node opts conf]
  (let [{:keys [path tag value read-only?]} node
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
  (let [{:keys [path tag value choices]} node
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
         (name (:name choice))])]]))

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
                    :disabled true}])

(defmethod form-component :ref
  [node opts]
  [input node opts {:disabled true}])

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
     [select node opts {:multiple true
                        :label-class "form__title"}]
     (for [[index choice] selected-choices]
       ^{:key (:name choice)}
       (form-component choice (update opts :path conj :choices index)))]))

(defmethod form-component :default
  [node _]
  [:span (:name node)])

(defn- event->js-files
  [event]
  (.. event  -target -files))

(defn- handle-file-upload
  [event]
  (let [js-file (-> event
                 event->js-files
                 (aget 0))]
    (rf/dispatch [::read-settings-from-file js-file])))

(defn- settings-file-loader []
  [:input.settings-file-loader
   {:id "settings-file-loader-input"
    :type "file"
    :multiple false
    :accept [".edn"]
    :on-change handle-file-upload}])

(defn root-component []
  (let [settings (rf/subscribe [::settings/settings])]
    (fn []
      (when (seq @settings)
        [:div.settings-editor
         [sidebar/main @settings]
         [:div.settings-editor__main
          (for [[index node] (keep-indexed vector @settings)]
            ^{:key (:name node)}
            (form-component node {:path [index]}))]
         [:div.settings-editor__footer
          [:div.settings-file-loader
           (settings-file-loader)]
          [:button {:on-click #(rf/dispatch [::save-settings-to-file])}
           "Save settings"]]]))))

(rdom/render [root-component]
             (.getElementById js/document "app"))
