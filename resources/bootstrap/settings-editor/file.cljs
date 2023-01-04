(ns file
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))

(defn- write-file
  [file-name file-type data]
  (let [blob (js/Blob. #js [data] #js {:type file-type})
        object-url (js/URL.createObjectURL blob)
        anchor-element
        (doto (js/document.createElement "a")
          (-> .-href (set! object-url))
          (-> .-download (set! file-name)))]
    (.appendChild (.-body js/document) anchor-element)
    (.click anchor-element)
    (.removeChild (.-body js/document) anchor-element)
    (js/URL.revokeObjectURL object-url)))

(defn- js-file->file
  [file]
  (let [[name extension] (-> (.-name file)
                             (str/split #"\.(?!.*\.)"))
        type (.-type file)]
    (cond-> {:name name}
      extension
      (assoc :extension extension)
      type
      (assoc :type type))))

(defn- new-js-file-reader-obj
  []
  (new js/FileReader))

(defn- on-file-read
  [event file-data {:keys [save-fn]}]
  (let [file-content (-> event .-target .-result)]
    (save-fn (assoc file-data :content file-content))))

(defn- read-js-file
  [js-file config]
  (let [file (js-file->file js-file)
        reader (new-js-file-reader-obj)]
    (set! (.-onload reader)
          #(on-file-read % file config))
    (.readAsText reader js-file)))

(rf/reg-fx
 :write-to-file
 (fn [{:keys [file-name file-type data]}]
   (write-file file-name file-type data)))

(rf/reg-fx
 :read-from-file
 (fn [{:keys [js-file on-read-evt]}]
   (read-js-file
    js-file
    {:save-fn #(rf/dispatch (conj on-read-evt %))})))
