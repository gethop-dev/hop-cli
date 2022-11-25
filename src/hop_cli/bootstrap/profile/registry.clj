(ns hop-cli.bootstrap.profile.registry)

(defmulti pre-render-hook (fn [profile _settings] profile))
(defmethod pre-render-hook :default [_ _])

(defmulti post-render-hook (fn [profile _settings] profile))
(defmethod post-render-hook :default [_ _])
