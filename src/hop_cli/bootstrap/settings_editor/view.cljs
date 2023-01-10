(ns view
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::set-active-view
 (fn [db [_ view]]
   (assoc db :active-view view)))

(rf/reg-sub
 ::active-view
 (fn [db]
   (get db :active-view)))

(rf/reg-fx
 :scroll-to-element
 (fn [element-id]
   (when-let [element (js/document.getElementById element-id)]
     (.scrollIntoView element #js {:behavior "smooth"}))))

(rf/reg-event-fx
 ::scroll-to-element
 (fn [_ [_ element-id]]
   {:scroll-to-element element-id}))
