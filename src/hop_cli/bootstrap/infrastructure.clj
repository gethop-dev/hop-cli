(ns hop-cli.bootstrap.infrastructure)

(defmulti provision-initial-infrastructure :cloud-provider)

(defmulti save-environment-variables :cloud-provider)
