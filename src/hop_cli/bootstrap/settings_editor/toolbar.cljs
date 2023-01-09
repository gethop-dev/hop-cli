(ns toolbar
  (:require [cljs.pprint :as pprint]
            [clojure.edn :as edn]
            [re-frame.core :as rf]
            [settings :as settings]
            [view :as view]))

(rf/reg-event-fx
 ::settings-file-loaded
 (fn [_ [_ {:keys [content] :as _file}]]
   {:fx [[:dispatch [::settings/set-settings (edn/read-string content)]]]}))

(rf/reg-event-fx
 ::save-settings-to-file
 (fn [{:keys [db]} _]
   (let [settings-data (settings/remove-paths (:settings db))
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

(defn- event->js-files
  [event]
  (.. event  -target -files))

(defn- handle-file-upload
  [event]
  (let [js-file (-> event
                    event->js-files
                    (aget 0))]
    (rf/dispatch [::read-settings-from-file js-file])))

(defn- settings-file-import-btn []
  [:div.settings-editor__file-loader
   [:label.btn.settings-editor__file-loader-label
    {:for "settings-file-loader-input"}
    "Import settings"]
   [:input.settings-editor__file-loader-input
    {:id "settings-file-loader-input"
     :type "file"
     :multiple false
     :accept [".edn"]
     :on-change handle-file-upload}]])

(defn- settings-form-valid?
  []
  (let [js-element (js/document.getElementById "settings-editor-form")]
    (.checkValidity js-element)))

(defn- settings-file-export-btn
  [active-view]
  (let [disabled? (not= active-view :editor)]
    [:button.btn
     {:class (when disabled? "btn--disabled")
      :disabled disabled?
      :on-click
      (fn [_]
        (if (settings-form-valid?)
          (rf/dispatch [::save-settings-to-file])
          (js/alert "Some setting configuration option values are invalid.")))}
     "Export settings"]))

(defn main
  [active-view]
  [:div.settings-editor__toolbar
   [:h1.settings-editor__title "HOP CLI Settings Editor"]
   [:div.settings-editor__control-buttons
    [:button.btn {:on-click #(rf/dispatch [::view/set-active-view :profile-picker])}
     "Edit selected profiles"]
    [settings-file-import-btn]
    [settings-file-export-btn active-view]]])
