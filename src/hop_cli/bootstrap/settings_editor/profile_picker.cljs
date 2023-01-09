(ns profile-picker
  (:require [editor :as editor]
            [re-frame.core :as rf]
            [settings :as settings]
            [view :as view]))

(defn- profile-picker-handler
  [profiles-node]
  (fn [event-profile path e]
    (let [selected-profiles (:value profiles-node)
          dependees (:depends-on-profiles event-profile)
          dependent-profiles? (some (fn [{:keys [name depends-on-profiles]}]
                                      (and (get (set depends-on-profiles) (:name event-profile))
                                           (get (set selected-profiles) name)))
                                    (:choices profiles-node))
          checked? (.. e -target -checked)
          new-selected-profiles (cond
                                  checked?
                                  (distinct (concat selected-profiles [(:name event-profile)] dependees))

                                  dependent-profiles?
                                  selected-profiles

                                  :else
                                  (remove #{(:name event-profile)} selected-profiles))]
      (rf/dispatch [::settings/update-settings-value path new-selected-profiles]))))

(defn main
  []
  (let [settings (rf/subscribe [::settings/settings])]
    (fn []
      (let [profile-node-path (settings/get-node-path @settings [:project :profiles])
            profile-node (get-in @settings profile-node-path)]
        (when profile-node
          [:div.settings-editor__profile-picker
           [:h1 "Profiles selection"]
           [:span "Some profiles have dependencies over others. Therefore, the application will enforce their selection on your behalf."]
           [:div.settings-editor__profile-picker-choices
            [editor/checkbox-group profile-node {:path profile-node-path
                                                 :on-change-fn (profile-picker-handler profile-node)} {}]]
           [:button.btn {:on-click #(rf/dispatch [::view/set-active-view :editor])}
            "Next"]])))))
