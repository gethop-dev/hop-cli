;; This Source Code Form is subject to the terms of the MIT license.
;; If a copy of the MIT license was not distributed with this
;; file, You can obtain one at https://opensource.org/licenses/MIT
(ns user)

(defn dev
  "Load and switch to the 'dev' namespace."
  []
  (require 'dev)
  (in-ns 'dev)
  :loaded)
