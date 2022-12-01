;; This Source Code Form is subject to the terms of the MIT license.
;; If a copy of the MIT license was not distributed with this
;; file, You can obtain one at https://opensource.org/licenses/MIT

{{=<< >>=}}
(ns <<project.name>>.api.config-test
  (:require [clojure.test :refer :all]
            [<<project.name>>.test-utils :as util]
            [muuntaja.core :as m]
            [ring.mock.request :as mock]))

(use-fixtures :once
  (partial util/init-halt-system [:<<project.name>>.api/main]))

(deftest config-test
  (testing "Get config"
    (let [handler (get @util/system :<<project.name>>.api/main)
          req (-> (mock/request :get "/api/config")
                  (mock/header "accept" "application/transit+json"))
          response (handler req)
          decoded-body (m/decode-response-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success? decoded-body)))
      (is (map? (:config decoded-body))))))
