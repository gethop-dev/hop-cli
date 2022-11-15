(ns hop-cli.bootstrap.util)

(defn get-env-type
  [environment]
  (case environment
    :dev :to-develop
    :test :to-deploy
    :prod :to-deploy))
