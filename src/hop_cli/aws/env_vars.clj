;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

(ns hop-cli.aws.env-vars
  (:require [babashka.fs :as fs]
            [clojure.data :refer [diff]]
            [clojure.set :as set]
            [clojure.string :as str]
            [hop-cli.aws.api.eb :as api.eb]
            [hop-cli.aws.api.ssm :as api.ssm]
            [hop-cli.bootstrap.util :as bootstrap.util]
            [hop-cli.util.thread-transactions :as tht]
            [malli.core :as m])
  (:import (java.util Date)))

(def strings-env-vars-schema
  [:sequential
   [:and
    [:string {:min 1}]
    [:re #"^[A-Z_0-9]+=.+$"]]])

(def ^:const last-ssm-script-update-env-var
  "LAST_SSM_SCRIPT_ENV_UPDATE")

(def quoted-env-var-pattern
  (re-pattern #"^'.*'$"))

(defn- string-env-vars->env-vars
  [lines]
  (reduce (fn [env-vars line]
            (let [[k v] (str/split line #"=" 2)
                  v (if (re-matches quoted-env-var-pattern v)
                      (subs v 1 (dec (count v)))
                      v)]
              (assoc env-vars (keyword k) v)))
          {}
          lines))

(defn- get-env-vars-diff
  [ssm-env-vars file-env-vars]
  (let [keys-in-both (set/intersection (set (keys ssm-env-vars)) (set (keys file-env-vars)))
        [different-in-file different-in-ssm _] (diff file-env-vars ssm-env-vars)
        keys-to-create (set/difference (set (keys different-in-file)) keys-in-both)
        keys-to-update (set/intersection (set (keys different-in-file)) keys-in-both)
        keys-to-delete (set/difference (set (keys different-in-ssm)) keys-in-both)]
    {:to-create (select-keys file-env-vars keys-to-create)
     :to-update (select-keys file-env-vars keys-to-update)
     :to-delete (select-keys ssm-env-vars keys-to-delete)}))

(defn- sync-env-vars*
  [config {:keys [to-update to-create to-delete]}]
  (->
   [{:txn-fn
     (fn txn-1 [_]
       (let [result (api.ssm/put-env-vars-as-parameters config {:new? true} to-create)]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-create-env-vars
            :error-details result})))
     :rollback-fn
     (fn rollback-1 [prv-result]
       (let [result (api.ssm/delete-env-vars-parameters config to-create)]
         (when-not (:success? result)
           (prn "Create parameters rollback failed"))
         prv-result))}
    {:txn-fn
     (fn txn-2 [_]
       (let [result (api.ssm/put-env-vars-as-parameters config {} to-update)]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-update-env-vars
            :error-details result})))
     :rollback-fn
     (fn rollback-2 [prv-result]
       (prn "Missing rollback for updated parameters: " to-update)
       prv-result)}
    {:txn-fn
     (fn txn-2 [_]
       (let [result (api.ssm/delete-env-vars-parameters config to-delete)]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-delete-env-vars
            :error-details result})))}]
   (tht/thread-transactions {})))

(defn- non-env-var-line?
  [line]
  (or
   ;; Empty lines or with just whitespace
   (re-matches #"^\s*$" line)
   ;; Comment lines with optional initial whitespace
   (re-matches #"^\s*#.*$" line)))

(defn- read-env-vars-file
  [file]
  (try
    (->> file
         (fs/file)
         (fs/read-all-lines)
         (remove non-env-var-line?))
    (catch java.nio.file.NoSuchFileException _
      nil)))

(defn sync-env-vars
  [{:keys [file] :as config}]
  (if-let [strings-env-vars (read-env-vars-file file)]
    (if-not (m/validate strings-env-vars-schema strings-env-vars)
      {:success? false
       :reason :file-contains-invalid-environment-variable-format
       :error-details (m/explain strings-env-vars-schema strings-env-vars)}
      (let [result (api.ssm/get-env-vars-from-parameters config)]
        (if-not (:success? result)
          {:success? false
           :reason :could-not-get-ssm-env-vars
           :error-details result}
          (let [ssm-env-vars (:params result)
                file-env-vars (string-env-vars->env-vars strings-env-vars)
                env-vars-diff (get-env-vars-diff ssm-env-vars file-env-vars)
                result (sync-env-vars* config env-vars-diff)]
            (assoc result :sync-details env-vars-diff)))))
    {:success? false
     :error :file-not-found}))

(defn download-env-vars
  [{:keys [file] :as config}]
  (let [result (api.ssm/get-env-vars-from-parameters config)]
    (if-not (:success? result)
      {:success? false
       :reason :could-not-get-ssm-env-vars
       :error-details result}
      (let [result (->> (:params result)
                        (map bootstrap.util/env-var->string-env-var)
                        (bootstrap.util/write-variables-to-file! file))]
        {:success? (boolean result)}))))

(defn apply-env-var-changes
  [opts]
  (api.eb/update-env-variable (merge opts {:name last-ssm-script-update-env-var
                                           :value (.toString (Date.))})))
