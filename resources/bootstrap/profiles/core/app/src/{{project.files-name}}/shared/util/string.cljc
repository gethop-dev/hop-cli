;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.util.string
  #?(:clj (:import [java.text Normalizer Normalizer$Form])))

(defn ->nfc-normalized-unicode
  [unicode-str]
  #?(:clj (Normalizer/normalize unicode-str Normalizer$Form/NFC)
     :cljs (.normalize unicode-str "NFC")))
