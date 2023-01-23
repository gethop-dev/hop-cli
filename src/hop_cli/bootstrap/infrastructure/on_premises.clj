(ns hop-cli.bootstrap.infrastructure.on-premises
  (:require [hop-cli.bootstrap.infrastructure :as infrastructure]
            [hop-cli.bootstrap.util :as bp.util]))

(defmethod infrastructure/provision-initial-infrastructure :on-premises
  [settings]
  {:success? true
   :settings settings})

(defmethod infrastructure/save-environment-variables :on-premises
  [settings]
  {:success? true
   :saved-env-files (bp.util/write-environments-env-vars-to-file! settings)})
