;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.shared.localization.dictionaries
  (:require [<<project.name>>.shared.localization.dictionaries.en :as dictionaries.en]))

(def dictionaries
  {:en dictionaries.en/registry})

(def ^:const default-language :en)
