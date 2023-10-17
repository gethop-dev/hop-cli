;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.api.muuntaja.instance
  (:require [<<project.name>>.shared.util.transit :as util.transit]
            [muuntaja.core :as muuntaja]))

(defn build-muuntaja-instance
  []
  (-> muuntaja/default-options
      (update-in [:formats "application/transit+json" :encoder-opts :handlers]
                 merge util.transit/custom-write-handlers)
      (update-in [:formats "application/transit+json" :decoder-opts :handlers]
                 merge util.transit/custom-read-handlers)
      (muuntaja/create)))
