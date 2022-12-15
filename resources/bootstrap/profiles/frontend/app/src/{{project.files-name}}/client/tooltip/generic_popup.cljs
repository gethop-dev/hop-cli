;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.tooltip.generic-popup
  (:require [<<project.name>>.client.localization :refer [t]]
            [<<project.name>>.client.tooltip :as tooltip]
            [re-frame.core :as rf]))

(def ^:const popup-id "generic-popup")

(def ^:const ^:private default-data
  {:modal? false
   :lightbox? true
   :argv []})

(defn main []
  (let [popup-data (rf/subscribe [::tooltip/by-id popup-id])]
    (fn []
      (when (:component @popup-data)
        (let [{:keys [modal? lightbox? component argv on-destroy-evt
                      closable?]
               :or {closable? true}}
              (merge default-data @popup-data)]
          [:div.generic-popup__backdrop
           (merge
            (when modal? {:on-click #(.stopPropagation %)})
            (when lightbox? {:class "generic-popup__backdrop--lightbox"}))
           [:div.generic-popup__container
            {:class (tooltip/gen-controller-class popup-id)}
            (when closable?
              [tooltip/close-popup-component
               {:id popup-id
                :on-destroy-evt on-destroy-evt}])
            (into [component] argv)]])))))

(defn generic-confirmation-popup
  "Generic confirmation popup that work as primary dialog
   It accepts direct title and body texts or if not provided it switches to respective translation keywords.
   It accepts translation keywords for the action buttons or uses default ones if not provided.
   For not making too complex the on accept callback, it only allows a single argument, so use a map with all
   the arguments needed for the function to be flexible enough.
   For proper usage, pass arguments through `argv` property of tooltip config, not directly to the component."
  []
  (fn [{:keys [title title-transl-kw body body-transl-kw
               deny-transl-kw accept-transl-kw accept-callback-fn accept-callback-fn-arg
               only-acceptance?]}]
    [:div.generic-confirmation-popup
     [tooltip/close-popup-component {:id popup-id}]
     [:h1.generic-confirmation-popup__title
      (or title
          (t [title-transl-kw]))]
     (when (or body body-transl-kw)
       [:span.generic-confirmation-popup__body
        (or body
            (t [body-transl-kw]))])
     [:div.generic-confirmation-popup__buttons
      [:button.btn.generic-confirmation-popup__accept-btn
       {:on-click #(do
                     (if-not accept-callback-fn-arg
                       (accept-callback-fn)
                       (accept-callback-fn accept-callback-fn-arg))
                     (rf/dispatch [::tooltip/register {:id popup-id}]))}
       (t [(or accept-transl-kw
               :common/yes)])]
      (when-not only-acceptance?
        [:button.btn.btn--bordered
         {:on-click #(rf/dispatch [::tooltip/destroy-by-id {:id popup-id}])}
         (t [(or deny-transl-kw
                 :common/no)])])]]))
