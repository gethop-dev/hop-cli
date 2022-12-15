;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.service.timbre
  (:require [clojure.string :as str]
            [integrant.core :as ig]
            [taoensso.encore :as enc]
            [taoensso.timbre :as timbre]))

(def ^:private custom-config
  (atom {}))

(defn get-config
  "Get current custom timbre dynamic configuration.

  This custom configuration will be merged to the default timbre
  configuration, before the log entries are processed."
  []
  {:success? true
   :config @custom-config})

(defn set-config
  "Set a new custom timbre dynamic configuration.

  This custom configuration will be merged to the default timbre
  configuration, before the log entries are processed."
  [config]
  (reset! custom-config config)
  {:success? true})

(defn- dynamic-log-level
  [{:keys [level ?ns-str config] :as appender-data}]
  (let [log-config (merge config @custom-config)]
    (when (timbre/may-log? level level ?ns-str log-config)
      appender-data)))

(defmethod ig/init-key :<<project.name>>.service.timbre/middleware.dynamic-log-level [_ _]
  dynamic-log-level)

(defn- inject-git-tag
  [git-tag]
  (fn [appender-data]
    (assoc appender-data :git-tag git-tag)))

(defmethod ig/init-key :<<project.name>>.service.timbre/middleware.inject-git-tag [_ config]
  (inject-git-tag (:git-tag config)))

(defn- output-fn-with-git-tag
  "Default (fn [data]) -> string output fn.
  Use`(partial default-output-fn <opts-map>)` to modify default opts.

  Based on `taoenso.timbre/default-output-fn`"
  ([data] (output-fn-with-git-tag nil data))
  ([opts data] ; For partials
   (let [{:keys [no-stacktrace?]} opts
         {:keys [level ?err msg_ ?ns-str ?file hostname_
                 timestamp_ ?line git-tag]} data]
     (str
      (force timestamp_)
      " "
      (force hostname_) " "
      (when git-tag
        "(git-tag: ") git-tag ") "
      (str/upper-case (name level))  " "
      "[" (or ?ns-str ?file "?") ":" (or ?line "?") "] - "
      (force msg_)
      (when-not no-stacktrace?
        (when-let [err ?err]
          (str enc/system-newline (timbre/stacktrace err opts))))))))

(defmethod ig/init-key :<<project.name>>.service.timbre/output-fn-with-git-tag [_ _]
  output-fn-with-git-tag)
