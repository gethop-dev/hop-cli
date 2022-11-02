(ns hop-cli.aws.ssl
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [hop-cli.aws.acm.certificate :as acm.certificate])
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
  []
  (let [key-file (File/createTempFile "hop" ".pem")
        cert-file (File/createTempFile "hop" ".pem")]
    (try
      (let [result (create-self-signed-certificate key-file cert-file)]
        (if-not (:success? result)
          result
          (let [opts {:certificate (slurp cert-file)
                      :private-key (slurp key-file)}
                result (acm.certificate/import-certificate opts)]
            (if (:success? result)
              result
              {:success? false
               :reason :could-not-import-certificate
               :error-details result}))))
      (finally
        (.delete key-file)
        (.delete cert-file)))))
