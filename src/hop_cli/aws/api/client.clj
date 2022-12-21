;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.api.client
    (:require [com.grzm.awyeah.client.api :as aws]))

(defn gen-client
      [service-kw {:keys [region]}]
      (aws/client (merge {:api service-kw}
                         (when region
                               {:region region}))))
