(ns profile-picker
  (:require [editor :as editor]
            [re-frame.core :as rf]
            [settings :as settings]
            [toolbar :as toolbar]
            [view :as view]))

(defn- get-dependent-profiles-error-msg
  [dependent-profiles]
  (with-out-str
    (println "This profile can not be unchecked because there are one or more dependent profiles.")
    (println)
    (println "Please uncheck the dependent profiles first before unchecking this one.")
    (println)
    (println "Dependent profiles:")
    (doseq [{:keys [tag]} dependent-profiles]
      (println "  - " tag))))

(defn- dependent-profile?
  [event-profile selected-profiles {:keys [name depends-on-profiles]}]
  (and (get (set depends-on-profiles) (:name event-profile))
       (get (set selected-profiles) name)))

(defn- profile-picker-handler
  [profiles-node]
  (fn [event-profile path e]
    (let [selected-profiles (:value profiles-node)
          dependees (:depends-on-profiles event-profile)
          dependent-profiles (filter (partial dependent-profile?
                                              event-profile
                                              selected-profiles)
                                     (:choices profiles-node))
          checked? (.. e -target -checked)
          new-selected-profiles (cond
                                  checked?
                                  (distinct (concat selected-profiles [(:name event-profile)] dependees))

                                  (seq dependent-profiles)
                                  selected-profiles

                                  :else
                                  (remove #{(:name event-profile)} selected-profiles))]
      (when (and (not checked?) (seq dependent-profiles))
        (js/alert (get-dependent-profiles-error-msg dependent-profiles)))
      (rf/dispatch [::settings/update-settings-value path new-selected-profiles]))))

(defn main
  []
  (let [settings (rf/subscribe [::settings/settings])]
    (fn []
      (let [profile-node-path (settings/get-node-path @settings [:project :profiles])
            profile-node (get-in @settings profile-node-path)]
        (when profile-node
          [:div.profile-picker
           [:div]
           [toolbar/main
            {:title "Profiles selection"
             :subtitle "HOP CLI Settings Editor"
             :active-view :profile-picker}]
           [:span "Some profiles depend on others. Therefore, the application will enforce their selection on your behalf."]
           [editor/checkbox-group profile-node
            {:on-change-fn (profile-picker-handler profile-node)
             :hide-label? true
             :choices-group-class "profile-picker__choices"
             :choice-class "profile-picker__choice"}
            {}]
           [:button.btn.profile-picker__next-btn
            {:on-click #(rf/dispatch [::view/set-active-view :editor])}
            "Next"]])))))
