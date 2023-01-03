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
  "Get the mimetype from the filename extension. Takes an optional map of
			 extensions to mimetypes that overrides values in the default-mime-types map."
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

(def default-port 8090)

(defn serve-settings-editor
  "Serves static assets using web server.
		Options:
			* `:port` - port"
  [{:keys [port] :as opts}]
  (let [port (or port default-port)
        base-path "bootstrap"
        settings-editor-dir (str base-path "/settings-editor")]
    (binding [*out* *err*]
      (println (str "Serving assets at http://localhost:" port)))
    (server/run-server
     (fn [{:keys [uri]}]
       (let [f (URLDecoder/decode uri)
             index-file (str settings-editor-dir "/index.html")]
         (cond
           (= "/" f)
           (response index-file)

           (= "/settings.edn" f)
           (response (str base-path f))

           :else
           (response (str settings-editor-dir f)))))
     opts)
    @(promise)))
