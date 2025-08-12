;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))

(defn hmac-sha256
  "Both `key` and `data` *must* be byte-arrays"
  [^bytes key ^bytes data]
  (let [algo "HmacSHA256"
        mac (Mac/getInstance algo)]
    (.init mac (SecretKeySpec. key algo))
    (.doFinal mac data)))

(defn cli-stdin-map->map
  [stdin-parameters]
  (reduce (fn [acc s]
            (let [[k v] (str/split s #"=" 2)]
              (assoc acc (keyword k) v)))
          {}
          stdin-parameters))

(defn update-map-vals
  ([m update-fn & {:keys [recursive?] :or {recursive? true}}]
   (if recursive?
     (walk/postwalk
      (fn [x] (if (and (map? x) (not (record? x)))
                (update-map-vals x update-fn :recursive? false) x)) m)
     (reduce-kv #(assoc %1 %2 (update-fn %3)) {} m))))

(defn get-version
  []
  (-> (io/resource "version.txt")
      slurp
      str/trim
      ;; NOTE: for testing during development.
      (str/replace #"-SNAPSHOT" "")))
