;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.util.random
  (:import [java.security SecureRandom]))

(def ^:private rng (SecureRandom.))

(def ^:private chs
  (map char (concat (range 48 58) ; 0-9
                    (range 65 91) ; A-Z
                    (range 97 123) ; a-z
                    [\~ \! \# \$ \% \^ \& \* \( \) \- \+ \=])))

(defn- generate-random-character
  []
  (nth chs (.nextInt rng (count chs))))

(defn generate-random-password
  [{:keys [length]}]
  (apply str (take length (repeatedly generate-random-character))))
