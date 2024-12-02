;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.json
  (:require #?(:clj [jsonista.core :as json])
            #?(:cljs [com.cognitect.transit.types :as transit.types])
            #?(:cljs [clojure.walk :as walk])))
#?(:cljs
   (defn- transform-keys
     [m transform-fn]
     (let [f (fn [[k v]] [(transform-fn k) v])]
       (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m))))

#?(:cljs
   ;; NOTE this is required for the JSON encoding to work.
   ;; If not, clj->js doesn't touch uuid values, and when
   ;; stringifying the are serialized as objects instead
   ;; of plain strings.
   ;;
   ;; Take into account that this extension affects the
   ;; 'clj->js' function globally, not just for the json
   ;; encoding.
   (extend-protocol IEncodeJS
     cljs.core/UUID
     (-clj->js [x] (str x))
     (-key->js [x] (str x))
     transit.types/UUID
     (-clj->js [x] (str x))
     (-key->js [x] (str x))))

(defn ->json
  ([coll]
   (->json coll {}))
  ([coll {:keys [encode-key-fn pretty?]
          :or {encode-key-fn name
               pretty? false}}]
   #?(:clj
      (let [mapper (json/object-mapper {:encode-key-fn encode-key-fn
                                        :pretty pretty?})]
        (json/write-value-as-string coll mapper))
      :cljs
      (let [stringify-fn (.-stringify js/JSON)]
        (-> (transform-keys coll encode-key-fn)
            (clj->js)
            (stringify-fn
             nil
             (when pretty?
               2)))))))

(defn <-json
  ([str]
   (<-json str {}))
  ([str {:keys [decode-key-fn]
         :or {decode-key-fn keyword}}]
   #?(:clj
      (let [mapper (json/object-mapper {:decode-key-fn decode-key-fn})]
        (json/read-value str mapper))
      :cljs
      (-> (.parse js/JSON str)
          (js->clj)
          (transform-keys decode-key-fn)))))
