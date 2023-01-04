(ns hop-cli.bootstrap.settings-editor
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :as server])
  (:import (java.net URLDecoder)))

(def ^{:doc "A map of file extensions to mime-types."}
  default-mime-types
  {"cljs" "application/x-scittle"
   "css" "text/css"
   "edn" "application/edn"
   "html" "text/html"})

;; https://github.com/ring-clojure/ring/blob/master/ring-core/src/ring/util/mime_type.clj
(defn- filename-ext
  "Returns the file extension of a filename or filepath."
  [filename]
  (when-let [ext (second (re-find #"\.([^./\\]+)$" filename))]
    (str/lower-case ext)))

(defn- ext-mime-type
  "Get the mimetype from the filename extension.
  Takes an optional map of extensions to mimetypes that overrides
  values in the default-mime-types map."
  ([filename]
   (ext-mime-type filename {}))
  ([filename mime-types]
   (let [mime-types (merge default-mime-types mime-types)]
     (mime-types (filename-ext filename)))))

(defn- response
  [path]
  (if-let [body (io/resource path)]
    {:status 200
     :headers {"Content-Type" (ext-mime-type path)}
     :body (slurp body)}
    {:status 404}))

(defn serve-settings-editor
  "Serves static assets using web server."
  [{:keys [port] :as opts}]
  (let [src-path "hop_cli/bootstrap/settings_editor"
        resources-path "bootstrap/settings-editor"]
    (binding [*out* *err*]
      (println (str "Settings Editor running at http://localhost:" port)))
    (server/run-server
     (fn [{:keys [uri]}]
       (let [f (URLDecoder/decode uri)
             index-file (str resources-path "/index.html")]
         (cond
           (= "/" f)
           (response index-file)

           (= "/settings.edn" f)
           (response "bootstrap/settings.edn")

           (str/ends-with? f ".cljs")
           (response (str src-path f))

           :else
           (response (str resources-path f)))))
     opts)
    @(promise)))
