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

(defn generate-random-string
  [length]
  (apply str (take length (repeatedly generate-random-character))))
