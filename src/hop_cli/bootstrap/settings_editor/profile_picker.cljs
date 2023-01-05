(ns profile-picker
  (:require [editor :as editor]
            [re-frame.core :as rf]
            [settings :as settings]
            [view :as view]))

(defn main
  []
  (let [settings (rf/subscribe [::settings/settings])]
    (fn []
      (let [profile-node-path (settings/get-node-path @settings [:project :profiles])
            profile-node (get-in @settings profile-node-path)]
        (when profile-node
          [:div.settings-editor__profile-picker
           [:h1 "Profiles selection"]
           [:div.settings-editor__profile-picker-choices
            [editor/checkbox-group profile-node {:path profile-node-path} {}]]
           [:button.btn {:on-click #(rf/dispatch [::view/set-active-view :editor])}
            "Next"]])))))
