;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util
  (:refer-clojure :exclude [uuid]))

(defn uuid
  "If no argument is passed, creates a random UUID. If the passed
  paramenter is a UUID, returns it verbatim. If it is a string
  representing a UUID value return the corresponding UUID. Any other
  value or invalid string returns nil. "
  ([]
   (random-uuid))
  ([x]
   (try
     (cond
       (uuid? x)
       x

       (string? x)
       (parse-uuid x))
     (catch #?(:clj Throwable :cljs :default) _
       nil))))

(defn filter-map
  [m pred]
  (select-keys m (for [[k v] m :when (pred [k v])] k)))
