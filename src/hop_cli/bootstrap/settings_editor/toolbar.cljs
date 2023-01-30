;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns toolbar
  (:require [ajax.core :refer [POST]]
            [cljs.pprint :as pprint]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [settings :as settings]))

(rf/reg-fx
 :validate-and-patch-settings-file-content
 (fn [{:keys [content on-success-fn]}]
   (POST "/validate-and-patch"
     {:handler
      (fn [response]
        (on-success-fn (:settings (edn/read-string response))))
      :error-handler (fn [{:keys [response]}]
                       (if (= (:reason (edn/read-string response)) :incompatible-cli-and-settings-version)
                         (js/alert "Your settings file version is incompatible with your current HOP CLI version.")
                         (js/alert "Something went wrong!")))
      :body content})))

(rf/reg-event-fx
 ::settings-file-loaded
 (fn [_ [_ {:keys [content] :as _file}]]
   {:fx [[:validate-and-patch-settings-file-content {:content content
                                                     :on-success-fn #(rf/dispatch [::settings/set-settings %])}]]}))

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
 ::save-settings-errors-to-file
 (fn [_ [_ errors]]
   (let [file-name "settings-errors.txt"
         file-type "text/plain"]
     {:fx [[:write-to-file {:file-name file-name
                            :file-type file-type
                            :data errors}]]})))

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
                        (let [ref-path (settings/get-path-from-ref-node ref-node)
                              result (settings/lookup-ref settings ref-path)]
                          (assoc result :node ref-node)))
                      refs)]
    (if (every? :success? results)
      {:success? true}
      {:success? false
       :error-details (remove :success? results)})))

(defn- get-unresolved-refs-error-msg
  []
  (with-out-str
    (println "Some references can not be resolved. This might be caused by invalid configuration options' values or missing configuration.")
    (println)
    (println "Please take a look at downloaded file for a full list of errors.")))

(defn- get-unresolved-refs-errors-msgs
  [ref-nodes]
  (with-out-str
    (println "Reference errors found in:")
    (doseq [{:keys [node-name-path] :as ref-node} ref-nodes]
      (println)
      (println (str/join " → " (map name node-name-path)))
      (println "The reference above is pointing to an invalid value or unconfigured option in:")
      (println (str/join " → " (map name (settings/get-path-from-ref-node ref-node)))))))

(defn- settings-file-export-btn
  [active-view]
  (let [disabled? (not= active-view :editor)]
    [:button.btn.btn--flat.toolbar__btn
     {:class (when disabled? "btn--disabled")
      :disabled disabled?
      :on-click
      (fn [_]
        (let [settings @(rf/subscribe [::settings/settings])
              {:keys [success? error-details]} (lookup-selected-refs settings)]
          (cond
            (not (settings-form-valid?))
            (js/alert "Some setting configuration option values are invalid.")

            (not success?)
            (do
              (rf/dispatch [::save-settings-errors-to-file
                            (get-unresolved-refs-errors-msgs (mapv :node error-details))])
              (js/alert (get-unresolved-refs-error-msg)))

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
