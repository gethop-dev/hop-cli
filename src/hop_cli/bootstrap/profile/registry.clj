;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.bootstrap.profile.registry)

(defmulti pre-render-hook (fn [profile _settings] profile))
(defmethod pre-render-hook :default [_ _])

(defmulti post-render-hook (fn [profile _settings] profile))
(defmethod post-render-hook :default [_ _])
