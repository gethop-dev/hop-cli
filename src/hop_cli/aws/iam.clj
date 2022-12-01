(ns hop-cli.aws.iam
  (:require [hop-cli.aws.api.iam :as api.iam]))

(defn create-access-key
  [opts]
  (api.iam/create-access-key opts))
