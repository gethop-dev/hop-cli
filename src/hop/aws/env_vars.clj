(ns hop.aws.env-vars
  (:require [babashka.fs :as fs]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [hop.aws.ssm.eb :as ssm.eb]
            [hop.aws.ssm.parameter-store :as ssm.parameter-store]
            [hop.util.thread-transactions :as tht]
            [malli.core :as m])
  (:import (java.util Date)))

(def string-env-vars-schema
  [:sequential
   [:and
    [:string {:min 1}]
    [:re #"^[A-Z_0-9]+=.+$"]]])

(def ^:const last-ssm-script-update-env-var
  "LAST_SSM_SCRIPT_ENV_UPDATE")

(defn- string-env-var->env-var
  [s]
  (zipmap [:name :value] (str/split s #"=" 2)))

(defn- env-var->string-env-var
  [{:keys [name value]}]
  (format "%s=%s" name value))

(defn- get-env-var-diff
  [ssm-env-vars file-env-vars]
  (let [all-env-var-names (distinct (map :name (concat ssm-env-vars file-env-vars)))]
    (reduce
     (fn [r name]
       (let [in-ssm (some #(when (= name (:name %)) %) ssm-env-vars)
             in-file (some #(when (= name (:name %)) %) file-env-vars)]
         (cond
           (and in-file (not in-ssm))
           (update r :to-create conj in-file)

           (and in-ssm (not in-file))
           (update r :to-delete conj in-ssm)

           (and in-file in-ssm (not= (:value in-ssm) (:value in-file)))
           (update r :to-update conj in-file)

           :else
           r)))
     {:to-update []
      :to-create []
      :to-delete []}
     all-env-var-names)))

(defn- sync-env-vars*
  [config {:keys [to-update to-create to-delete]}]
  (->
   [{:txn-fn
     (fn txn-1 [_]
       (let [result (ssm.parameter-store/put-parameters config {:new? true} to-create)]
         (if (:success? result)
           {:success? true}
           {:success? false
            :reason :could-not-create-env-vars
            :error-details result})))
     :rollback-fn
     (fn rollback-1 [prv-result]
       (let [result (ssm.parameter-store/delete-parameters config to-create)]
         (when-not (:success? result)
           (prn "Create parameters rollback failed"))
         prv-result))}
    {:txn-fn
     (fn txn-2 [_]
       (let [result (ssm.parameter-store/put-parameters config {} to-update)]
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
       (let [result (ssm.parameter-store/delete-parameters config to-delete)]
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

(defn sync-env-vars
  [{:keys [file] :as config}]
  (let [string-env-vars (->>
                         (fs/file file)
                         (fs/read-all-lines)
                         (remove non-env-var-line?))]
    (if-not (m/validate string-env-vars-schema string-env-vars)
      (do
        (prn "File contains invalid environment variable format: ")
        (pprint/pprint (m/explain string-env-vars-schema string-env-vars)))
      (let [result (ssm.parameter-store/get-parameters config)]
        (if-not (:success? result)
          {:success? false
           :reason :could-not-get-ssm-env-vars
           :error-details result}
          (let [ssm-env-vars (:params result)
                file-env-vars (map string-env-var->env-var string-env-vars)
                env-var-diff (get-env-var-diff ssm-env-vars file-env-vars)
                result (sync-env-vars* config env-var-diff)]
            (assoc result :sync-details env-var-diff)))))))

(defn download-env-vars
  [{:keys [file] :as config}]
  (let [result (ssm.parameter-store/get-parameters config)]
    (if-not (:success? result)
      {:success? false
       :reason :could-not-get-ssm-env-vars
       :error-details result}
      (let [result (->> (:params result)
                        (map env-var->string-env-var)
                        (fs/write-lines file))]
        {:success? (boolean result)}))))

(defn apply-env-var-changes-handler
  [config]
  (ssm.eb/update-env-variable config {:name last-ssm-script-update-env-var
                                      :value (.toString (Date.))}))
