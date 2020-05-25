(ns toyokumo.commons.db
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]
   [schema.core :as s]
   [toyokumo.commons.csv :as tc.csv])
  (:import
   (com.zaxxer.hikari.pool
    HikariProxyConnection)
   (java.io
    InputStream
    Reader
    StringReader)
   (org.postgresql.copy
    CopyManager)
   (org.postgresql.core
    BaseConnection)))

;;; Customize points

(def ^:dynamic *label-fn*
  "How to format columns
  Caution!
    (->kebab-case \"foo1\") => foo-1"
  csk/->kebab-case)

(def ^:dynamic *make-sqlvec*
  "How to make sqlvec
  You may want to change it to formatting sql function that a sql library provides"
  identity)

(def ^:dynamic *format-table*
  "How to format table name
  Caution!
    (->snake_case \"foo1\") => foo_1"
  (comp csk/->snake_case str/lower-case name))

(def ^:dynamic *format-column*
  "How to format column name
  Caution!
    (->snake_case \"foo1\") => foo_1"
  (comp csk/->snake_case str/lower-case name))

;;; Process ResultSet

(defn- rs-builder
  [rs opts]
  (jdbc.rs/as-unqualified-modified-maps rs (assoc opts :label-fn *label-fn*)))

;;; APIs

(s/defn fetch
  "execute select query and get all result rows as a sequence of map

  See for opts https://github.com/seancorfield/next-jdbc/blob/master/doc/all-the-options.md"
  ([ds sql]
   (fetch ds sql nil))
  ([ds sql opts]
   (let [sqlvec (*make-sqlvec* sql)]
     (->> (jdbc/execute! ds sqlvec (merge {:builder-fn rs-builder} opts))
          (map #(into {} %))))))

(s/defn fetch-one
  "execute select query and get a first row as a map

  See for opts https://github.com/seancorfield/next-jdbc/blob/master/doc/all-the-options.md"
  ([ds sql]
   (fetch-one ds sql nil))
  ([ds sql opts]
   (let [sqlvec (*make-sqlvec* sql)]
     (some->> (jdbc/execute-one! ds sqlvec (merge {:builder-fn rs-builder} opts))
              (into {})))))

(s/defn execute :- [{s/Keyword s/Any}]
  "execute insert, update or delete query and get all effected rows as a sequence of map

  See for opts https://github.com/seancorfield/next-jdbc/blob/master/doc/all-the-options.md"
  ([ds sql]
   (execute ds sql nil))
  ([ds sql opts]
   (let [sqlvec (*make-sqlvec* sql)]
     (->> (jdbc/execute! ds sqlvec (merge {:builder-fn rs-builder}
                                          opts
                                          {:return-keys true}))
          (map #(into {} %))))))

(s/defn execute-one :- (s/maybe {s/Keyword s/Any})
  "execute insert, update or delete query and get a first row as a map

  See for opts https://github.com/seancorfield/next-jdbc/blob/master/doc/all-the-options.md"
  ([ds sql]
   (execute-one ds sql nil))
  ([ds sql opts]
   (let [sqlvec (*make-sqlvec* sql)]
     (some->> (jdbc/execute-one! ds sqlvec (merge {:builder-fn rs-builder}
                                                  opts
                                                  {:return-keys true}))
              (into {})))))

(s/defn execute-batch :- (s/maybe s/Int)
  "execute insert, update or delete query and get the effected number

  See for opts https://github.com/seancorfield/next-jdbc/blob/master/doc/all-the-options.md"
  ([ds sql]
   (execute-batch ds sql nil))
  ([ds sql opts]
   (let [sqlvec (*make-sqlvec* sql)]
     (->> (jdbc/execute-one! ds sqlvec (merge opts {:return-keys false}))
          ::jdbc/update-count))))

(defn- copy-in*
  [^HikariProxyConnection conn
   ^String sql
   ^Reader reader]
  (let [cm (CopyManager. (.unwrap conn BaseConnection))]
    (.copyIn cm sql reader)))

(s/defn copy-in :- s/Int
  "Use COPY FROM STDIN for very fast copying from a Reader into a database table

  ds      - implements next.jdbc.protocols/Sourceable or connection
  table   - table name. e.g. :my-data
  columns - column names. e.g. [:foo :bar :baz]
  values  - java.io.Reader, java.io.InputStream, String or sequence of sequences
            which represents CSV data like [[1 \"abc\" \"def\"]]"
  [ds table columns values]
  (let [table (*format-table* table)
        columns (map *format-column* columns)
        sql (format "COPY %s (%s) FROM STDIN (FORMAT CSV)"
                    table (str/join "," columns))]
    (with-open [conn (jdbc/get-connection ds)]
      (cond
        (instance? Reader values)
        (copy-in* conn sql values)

        (instance? InputStream values)
        (with-open [reader (io/reader values)]
          (copy-in* conn sql reader))

        (string? values)
        (with-open [reader (StringReader. values)]
          (copy-in* conn sql reader))

        :else
        (with-open [reader (let [sb (StringBuilder.)
                                 printer (tc.csv/csv-printer sb {:format :postgresql-csv})]
                             (tc.csv/write-all printer values)
                             (StringReader. (str sb)))]
          (copy-in* conn sql reader))))))

(defmacro with-transaction
  "This macro is completely same as the next.jdbc/with-transaction.
  It's copied in order that clients can only require this ns for DB access.

  ===Original doc===
  Given a transactable object, gets a connection and binds it to `sym`,
  then executes the `body` in that context, committing any changes if the body
  completes successfully, otherwise rolling back any changes made.

  The options map supports:
  * `:isolation` -- `:none`, `:read-committed`, `:read-uncommitted`,
      `:repeatable-read`, `:serializable`,
  * `:read-only` -- `true` / `false`,
  * `:rollback-only` -- `true` / `false`.
  ==================="
  [[sym transactable opts] & body]
  (let [con (vary-meta sym assoc :tag 'java.sql.Connection)]
    `(jdbc/transact ~transactable (^{:once true} fn* [~con] ~@body) ~(or opts {}))))
