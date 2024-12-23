;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/

{{=<< >>=}}
(ns <<project.name>>.boundary.adapter.persistence.sql.jdbc-util
  (:require [<<project.name>>.domain.types :as dom.types]
            [<<project.name>>.shared.util.json :as util.json]
            [camel-snake-kebab.core :refer [->kebab-case ->kebab-case-keyword
                                            ->snake_case]]
            [camel-snake-kebab.extras :as cske]
            [clojure.string :as str]
            [duct.logger :refer [log]]
            [malli.core :as m]
            [malli.transform :as mt]
            [medley.core :as medley]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]
            [next.jdbc.prepare :as jdbc.prepare]
            [next.jdbc.quoted :as jdbc.quoted]
            [next.jdbc.result-set :as rs])
  (:import [clojure.lang IPersistentMap IPersistentVector Keyword]
           [java.lang String]
           [java.sql PreparedStatement]
           [org.postgresql.jdbc PgResultSetMetaData]
           [org.postgresql.util PGobject PSQLException]))

(set! *warn-on-reflection* true)

(defn- elapsed [start]
  (/ (double (- (System/nanoTime) start)) 1000000.0))

(defn- sql-logger
  [_logger _operation sql-statement]
  {:sql-statement sql-statement
   :start-time (System/nanoTime)})

(defn- result-logger
  [logger operation state result]
  (let [elapsed-time (elapsed (:start-time state))
        log-details {:msec elapsed-time
                     :operation-type operation
                     :sql-statement (:sql-statement state)}]
    (if (instance? Throwable result)
      (log logger :error :jdbc-util/sql-error
           (assoc log-details :ex-message (ex-message result)))
      (log logger :trace :jdbc-util/sql-success
           (assoc log-details :result result)))))

(defn with-jdbc-utils
  [spec logger]
  (-> spec
      (jdbc/with-logging (partial sql-logger logger) (partial result-logger logger))
      (jdbc/with-options {:column-fn (comp jdbc.quoted/postgres
                                           #(->snake_case % :separator \-))
                          :table-fn (comp jdbc.quoted/postgres
                                          #(->snake_case % :separator \-))
                          :label-fn #(->kebab-case % :separator \_)
                          :builder-fn rs/as-unqualified-modified-maps})))

(defn column-names->p-column-names
  [tuple]
  (cske/transform-keys ->snake_case tuple))

(def ^:const integrity-constraint-violation-state-codes
  {:integrity-constraint "23000"
   :restrict "23001"
   :not-null "23502"
   :foreign-key "23503"
   :unique "23505"
   :check "23514"
   :exclusion "23P01"})

(defn- is-constraint-violation?
  [^PSQLException exception constraint]
  (and (= (get integrity-constraint-violation-state-codes (:type constraint))
          (.getSQLState exception))
       (= (:name constraint) (-> exception
                                 .getServerErrorMessage
                                 .getConstraint))))

(defn with-constraint-violation-check-fn
  [constraints body-fn]
  (try
    (body-fn)
    (catch PSQLException e
      (if-let [reason (:error-reason (medley/find-first
                                      (partial is-constraint-violation? e)
                                      constraints))]
        {:success? false
         :reason reason
         :error-details {:msg (ex-message e)}}
        {:success? false
         :reason :unknown-sql-error
         :error-details {:msg (ex-message e)}}))
    (catch Throwable t
      {:success? false
       :reason :exception
       :error-details {:msg (ex-message t)}})))

(defmacro with-constraint-violation-check
  [constraints & body]
  `(with-constraint-violation-check-fn
     ~constraints
     (fn [] ~@body)))

(defn decode-json-value
  [schema value]
  (m/decode
   schema
   (cske/transform-keys ->kebab-case value)
   mt/string-transformer))

(defn decode-json-sequence-value
  [schema value]
  (keep
   (partial decode-json-value schema)
   value))

(defn- ->pgjson
  [x pgjson-type]
  (doto (PGobject.)
    (.setType (or (:pgtype (meta x)) (name pgjson-type)))
    (.setValue (util.json/->json x))))

(defmulti ->pgvalue
  (fn [_ pgtype] pgtype))

(defmethod ->pgvalue :json
  [x pgtype]
  (->pgjson x pgtype))

(defmethod ->pgvalue :default
  [x _]
  x)

(defmulti <-pgvalue (fn [v _] (class v)))

(defmethod <-pgvalue String
  [v pgtype]
  (if (and v
           ;; When using plans and reducibles pgtype might be nil for
           ;; string values. In that case don't even try to apply any
           ;; transformations.
           pgtype
           (not= pgtype "text")
           (->> (str/replace pgtype #"_" "-")
                (keyword)
                (get dom.types/enum-types)))
    (keyword v)
    v))

(defmethod <-pgvalue PGobject
  [^PGobject v _]
  (let [type (.getType v)
        value (.getValue v)]
    (if (and value (get #{"json" "jsonb"} type))
      (with-meta (util.json/<-json value {:decode-key-fn ->kebab-case-keyword}) {:pgtype type})
      value)))

;; NOTE
;; Requiring the 'jdbc.date-time' namespace setups the SettableParameter
;; extension to convert java.time.Instant's into the SQL equivalent when
;; writing into the database.
;;
;; For the inverse operation the ReadableColumn protocol has to be extended.
;; next.jdbc provides three different extensions that are exposed as
;; functions. We are interested in 'read-as-instant', so that's why we need
;; to call the function here.
(jdbc.date-time/read-as-instant)

(extend-protocol jdbc.prepare/SettableParameter
  IPersistentMap
  (set-parameter [^IPersistentMap m ^PreparedStatement stm ^Integer ix]
    (.setObject stm ix (->pgvalue m :json)))

  IPersistentVector
  (set-parameter [^IPersistentVector v ^PreparedStatement stm ^Integer ix]
    (.setObject stm ix (->pgvalue v :json)))

  Keyword
  (set-parameter [^Keyword v ^PreparedStatement stm ^Integer ix]
    (.setObject stm ix ^Object (name v) java.sql.Types/OTHER)))

(extend-protocol rs/ReadableColumn
  String
  (read-column-by-label [^String v _]
    (<-pgvalue v nil))
  (read-column-by-index [^String v ^PgResultSetMetaData rsmeta ^Integer index]
    (<-pgvalue v (.getColumnTypeName rsmeta index)))
  PGobject
  (read-column-by-label [^PGobject v _]
    (<-pgvalue v nil))
  (read-column-by-index [^PGobject v ^PgResultSetMetaData rsmeta ^Integer index]
    (<-pgvalue v (.getColumnTypeName rsmeta index))))
