(ns profile-picker
  (:require [re-frame.core :as rf]
            [settings :as settings]
            [view :as view]))

(defn main
  []
  (let [settings (rf/subscribe [::settings/settings])]
    (fn []
      (let [profile-node-path (settings/get-node-path @settings [:project :profiles])
            profile-node (get-in @settings profile-node-path)
            profile-node-values (set (:value profile-node))]
        [:div.settings-editor__profile-picker
         [:h1 "Profiles selection"]
         [:div.settings-editor__profile-picker-choices
          (for [node (:choices profile-node)
                :let [node-path (conj profile-node-path (:name node))
                      node-id (settings/build-node-id node-path)]]
            [:div
             [:input {:id node-id
                      :type "checkbox"
                      :checked (boolean (get profile-node-values (:name node)))
                      :on-change (fn [_]
                                   (let [new-profile-node-values (settings/toggle-value profile-node-values (:name node))]
                                     (rf/dispatch [::settings/update-settings-value profile-node-path new-profile-node-values])))}]
             [:label {:for node-id} (:name node)]])]
         [:button.btn {:on-click #(rf/dispatch [::view/set-active-view :editor])}
          "Next"]]))))
