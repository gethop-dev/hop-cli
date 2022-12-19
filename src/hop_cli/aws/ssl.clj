;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.ssl
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [hop-cli.aws.api.acm :as api.acm])
  (:import [java.io File]))

(defn- create-self-signed-certificate
  [key-file cert-file]
  (let [result
        (apply
         shell/sh
         (str/split
          (format "openssl req -x509 -newkey rsa:4096 -keyout %s -out %s -sha256 -days 1825 -subj /CN=self-signed.invalid -nodes"
                  (.getPath key-file) (.getPath cert-file))
          #" "))]
    (if-not (= 0 (:exit result))
      {:success? false
       :reason :could-not-create-certificate
       :error-details result}
      {:success? true})))

(defn create-and-upload-self-signed-certificate
  [_]
  (let [key-file (File/createTempFile "hop" ".pem")
        cert-file (File/createTempFile "hop" ".pem")]
    (try
      (let [result (create-self-signed-certificate key-file cert-file)]
        (if-not (:success? result)
          result
          (let [opts {:certificate (slurp cert-file)
                      :private-key (slurp key-file)}
                result (api.acm/import-certificate opts)]
            (if (:success? result)
              result
              {:success? false
               :reason :could-not-import-certificate
               :error-details result}))))
      (finally
        (.delete key-file)
        (.delete cert-file)))))
