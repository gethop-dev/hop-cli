;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.client.session
  (:require [<<project.name>>.client.localization :as localization]
            [<<project.name>>.client.session.oidc-sso :as oidc-sso]
            [<<project.name>>.client.session.user :as user]
            [cljsjs.keycloak-js]
            [goog.object :as g]
            [re-frame.core :as rf]))

;; Keycloak Javascript library is not designed to be used in a
;; functional way. When you create a keycloak object to interact with
;; it, it keeps a lot of internal state that it needs to perform
;; operations like login state, logout, token refreshment, etc. If we
;; create a new object with the same configuration settings, we don't
;; get any of that internal state back. It's only available in the
;; original object. In practice, that means we need to keep a copy of
;; the original Keycloak object that we used to log in, so we can do
;; operations like logout.
;;
;; Because of the way re-frame recommends to design event handler
;; side-effects, we shouldn't build the Keycloak object in the event
;; handler (that would be side-effectful!). But if we build it in the
;; effect handler, we can't store in the appdb (it's not available
;; there). So after an internal discussion, we have decided that the
;; least hacky way of doing it is storing the Keycloak object in a
;; Reagent atom.
(defonce keycloak (atom nil))

(rf/reg-event-fx
 ::set-auth-error
 (fn [{:keys [db]} [_ error]]
   {:db (assoc db :auth-error error)}))

(rf/reg-sub
 ::auth-error
 (fn [db]
   (get db :auth-error)))

(defn- handle-keycloak-obj-change
  [keycloak-obj]
  (let [token-exp (g/getValueByKeys keycloak-obj "idTokenParsed" "exp")]
    ;; See comment at the top of this file to see
    ;; why we manage the keycloak object this way.
    (reset! keycloak keycloak-obj)
    (rf/dispatch [::oidc-sso/trigger-sso-apps])
    (rf/dispatch [::schedule-token-refresh token-exp])))

(defn- session-cofx
  [cofx _]
  (let [keycloak-state @keycloak
        session (when-let [jwt-token (g/getValueByKeys keycloak-state "idToken")]
                  (let [token-exp (g/getValueByKeys keycloak-state "idTokenParsed" "exp")
                        user-type (g/getValueByKeys keycloak-state "idTokenParsed" "user_type")]
                    {:jwt-token jwt-token
                     :token-exp token-exp
                     :user-type (keyword user-type)}))]
    (assoc cofx :session session)))

(rf/reg-cofx :session session-cofx)

(rf/reg-fx
 ::refresh-token-keycloak
 (fn [{:keys [min-validity]}]
   (let [keycloak-obj @keycloak]
     (-> keycloak-obj
         (.updateToken min-validity)
         (.then
          (fn [refreshed]
              ;; If token was still valid, do nothing
            (when refreshed
              (handle-keycloak-obj-change keycloak-obj))))
         (.catch
          (fn []
            (doseq [event [[::set-auth-error "Failed to refresh token, or the session has expired. Logging user out."]
                           [::user-logout]]]
              (rf/dispatch event))))))))

(rf/reg-event-fx
 ::refresh-token
 (fn [{:keys [db]} [_ min-validity]]
   {:db db
    ::refresh-token-keycloak {:min-validity min-validity}}))

(rf/reg-event-fx
 ::schedule-token-refresh
 (fn [{:keys [db]} [_ token-exp]]
   (let [now (/ (.getTime (js/Date.)) 1000)
         token-lifetime (int (- token-exp now))
         ;; If we refresh the token when it's close to the session
         ;; lifetime, keycloak returns a new token with a lifetime
         ;; that is the difference between the current time and the
         ;; session expiration time. Which may be lower than the
         ;; configured token lifetime. As we keep refreshing the token
         ;; the lifetime gets shorter and shorter, and eventually gets
         ;; smaller than 2 seconds. That means half-lifetime would be
         ;; zero. But half-lifetime must be greater than zero.
         ;; Otherwise :dispatch-later would get a zero min-delay
         ;; and return immediately without dispatching the event(s). So
         ;; make sure half-lifetime is at least 1 second.
         half-lifetime (max 1 (quot token-lifetime 2))
         min-validity token-lifetime]
     {:db db
      :dispatch-later [{:ms (* 1000 half-lifetime)
                        :dispatch [::refresh-token min-validity]}]})))

(rf/reg-fx
 :init-and-try-to-authenticate
 (fn [{:keys [config on-auth-success-evt on-auth-failure-evt]}]
   (let [{:keys [realm url client-id]} (get config :oidc)
         keycloak-obj (js/Keycloak #js {:realm realm
                                        :url url
                                        :clientId client-id})]
     (-> keycloak-obj
         (.init #js {"onLoad" "check-sso"
                     "promiseType" "native"
                     "silentCheckSsoRedirectUri" (str js/window.location.origin "/silent-check.html")})
         (.then (fn [authenticated]
                  (reset! keycloak keycloak-obj)
                  (if authenticated
                    (do
                      (handle-keycloak-obj-change keycloak-obj)
                      (rf/dispatch [::user/fetch-user-data :on-success-evt on-auth-success-evt]))
                    (rf/dispatch on-auth-failure-evt))))
         (.catch (fn [_]
                   (rf/dispatch [::set-auth-error "Failed to initialize Keycloak"])))))))

(rf/reg-fx
 ::login
 (fn [opts]
   (when @keycloak
     (.login @keycloak (clj->js opts)))))

(rf/reg-event-fx
 ::user-login
 (fn [{:keys [db]} [_ opts]]
   (let [language (localization/get-language db)]
     {::login (assoc opts :locale language)})))

(rf/reg-fx
 ::register
 (fn [opts]
   (when @keycloak
     (.register @keycloak (clj->js opts)))))

(rf/reg-event-fx
 ::user-register
 (fn [{:keys [db]} [_ opts]]
   (let [language (localization/get-language db)]
     {::register (assoc opts :locale language)})))

(rf/reg-fx
 ::logout
 (fn [_]
   (when @keycloak
     (.logout @keycloak)
     ;; See comment at the top of this file to see why we manage the
     ;; keycloak object this way.
     (reset! keycloak nil))))

(rf/reg-event-fx
 ::user-logout
 (fn [{:keys [db]} [_]]
   {::logout []
    :dispatch [::oidc-sso/trigger-logout-apps]}))

(rf/reg-fx
 ::manage-account
 (fn [_]
   (when @keycloak
     (.accountManagement @keycloak))))

(rf/reg-event-fx
 ::user-manage-account
 (fn [_ _]
   {::manage-account []}))
