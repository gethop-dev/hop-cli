(ns hop-cli.bootstrap.infrastructure)

(defmulti provision-initial-infrastructure :cloud-provider/value)

(defmulti save-environment-variables :cloud-provider/value)
