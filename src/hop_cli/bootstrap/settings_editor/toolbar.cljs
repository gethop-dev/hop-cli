(ns toolbar
  (:require [cljs.pprint :as pprint]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [settings :as settings]))

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
  [:div
   [:label.btn.btn--flat.toolbar__btn
    {:for "settings-file-loader-input"}
    [:img.toolbar__btn-icon
     {:src "img/import.svg"}]
    "Import settings"]
   [:input.toolbar__file-input
    {:id "settings-file-loader-input"
     :type "file"
     :multiple false
     :accept [".edn"]
     :on-change handle-file-upload}]])

(defn- settings-form-valid?
  []
  (let [js-element (js/document.getElementById "settings-editor-form")]
    (.checkValidity js-element)))

(defn lookup-selected-refs
  [settings]
  (let [refs (settings/get-selected-refs settings)
        results (mapv (fn [ref-node]
                        (let [result (settings/lookup-ref settings (settings/get-path-from-ref-node ref-node))]
                          (assoc result :node ref-node)))
                      refs)]
    (if (every? :success? results)
      {:success? true}
      {:success? false
       :error-details (remove :success? results)})))

(defn- settings-file-export-btn
  [active-view]
  (let [disabled? (not= active-view :editor)]
    [:button.btn.btn--flat.toolbar__btn
     {:class (when disabled? "btn--disabled")
      :disabled disabled?
      :on-click
      (fn [_]
        (let [settings @(rf/subscribe [::settings/settings])
              result (lookup-selected-refs settings)]
          (cond
            (not (settings-form-valid?))
            (js/alert "Some setting configuration option values are invalid.")

            (not (:success? result))
            (js/alert "Some setting references can't be resolved. Please check for invalid fields.")

            :else
            (rf/dispatch [::save-settings-to-file]))))}
     [:img.toolbar__btn-icon
      {:src "img/export.svg"}]
     "Export settings"]))

(defn main
  [{:keys [title subtitle active-view]}]
  [:div.toolbar
   [:div.toolbar__title-container
    [:h1.toolbar__title
     title]
    [:h2.toolbar__subtitle
     subtitle]]
   [settings-file-import-btn]
   [settings-file-export-btn active-view]])
