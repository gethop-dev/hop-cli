;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.middleware.authentication
  "This namespace contains the two middlewares that are used to
  handle Reitit Ring route authentication. There are two middlewares
  because of technical reasons, but they are supposed to be used
  together.

  The middlewares behaviour depends on the 'authentication' key
  specified in each route configuration. The value can be one of the
  followings:

  - ':none' : no authentication checks are performed.
  - ':optional': injects the middleware that processes the token (if present)
                 and adds the identity (if token present and valid) to the
                 request map.
  - 'required': injects the same middleware as the ':optional' one, plus a
                second one that enforces that the token is present and valid.
                If the token is missing or invalid it returns a 401 http error."
  (:require [<<project.name>>.api.util.responses :as util.r]
            [buddy.auth :refer [authenticated?]]
            [clojure.spec.alpha :as s]))

(s/def ::authentication #{:required :optional :none})

(def authentication-middleware-plugger
  {:name ::authentication
   ;; The spec is only checked if the 'compile' doesn't return nil.
   ;; In this case the spec is enforced unless 'authentication' is
   ;; set to 'none'. That means that all routes must define the
   ;; 'authentication' key.
   :spec (s/keys :req-un [::authentication])
   :compile
   (fn [route-config _opts]
     (let [authentication (get route-config :authentication)]
       (when-not (= :none authentication)
         (fn [handler auth-middleware]
           (auth-middleware handler)))))})

(def authentication-required-middleware
  {:name ::authentication-required
   :spec (s/keys :req-un [::authentication])
   :compile
   (fn [route-config _opts]
     (let [authentication (get route-config :authentication)]
       (when (= :required authentication)
         (fn [handler]
           (fn [req]
             (if (authenticated? req)
               (handler req)
               (util.r/unauthorized {:reason :invalid-or-missing-id-token})))))))})
