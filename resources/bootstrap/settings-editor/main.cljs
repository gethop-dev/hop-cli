(ns main
  (:require [ajax.core :refer [GET]]
            [cljs.pprint :as pprint]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [file]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(rf/reg-fx
 :do-get-request
 (fn [{:keys [uri handler-evt]}]
   (GET uri {:handler (fn [response]
                        (rf/dispatch (conj handler-evt response)))})))

(rf/reg-event-db
 ::set-settings
 (fn [db [_ settings]]
   (assoc db :settings settings)))

(rf/reg-event-fx
 ::default-settings-loaded
 (fn [_ [_ response]]
   {:fx [[:dispatch [::set-settings (edn/read-string response)]]]}))

(rf/reg-event-fx
 ::settings-file-loaded
 (fn [_ [_ {:keys [content] :as _file}]]
   {:fx [[:dispatch [::set-settings (edn/read-string content)]]]}))

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

(rf/reg-event-db
 ::update-settings-value
 (fn [db [_ path value]]
   (assoc-in db (cons :settings (conj path :value)) value)))

(rf/reg-sub
 ::settings-value
 (fn [db [_ path]]
   (get-in db (cons :settings path))))

(rf/reg-sub
 ::settings
 (fn [db _]
   (get db :settings)))

(defmulti form-component
  (fn [{:keys [type]} _opts]
    type))

(defmethod form-component :plain-group
  [node opts]
  (let [initial-path (:path opts)]
    [:div.plain-group
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
                       (rf/dispatch [::update-settings-value path value])))}
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
                       (rf/dispatch [::update-settings-value path value])))}
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
  (let [selected-choice (some (fn [choice]
                                (when (= (:name choice) (:value node))
                                  choice))
                              (:choices node))
        selected-choice-index (.indexOf (:choices node) selected-choice)]
    [:div.single-choice-group
     [select node opts {:label-class "form__title"}]
     (form-component selected-choice (update opts :path conj :choices selected-choice-index))]))

(defmethod form-component :multiple-choice-group
  [node opts]
  (let [selected-choices (filter (fn [[index choice]]
                                   (when (get (set (:value node)) (:name choice))
                                     [index choice]))
                                 (keep-indexed vector (:choices node)))]
    [:div.multiple-choice-group
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
  (let [settings (rf/subscribe [::settings])]
    (fn []
      (let [path [0]]
        (when (seq @settings)
          [:div
           [:div.settings-file-loader
            (settings-file-loader)]
           (form-component (get-in @settings path) {:path path})
           [:div.footer
            [:button {:on-click #(rf/dispatch [::save-settings-to-file])}
             "Save settings"]]])))))

(rdom/render [root-component]
             (.getElementById js/document "app"))
