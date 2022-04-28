(ns toyokumo.commons.db.postgresql
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [schema.core :as s]
   [toyokumo.commons.csv :as tc.csv])
  (:import
   (com.zaxxer.hikari.pool
    HikariProxyConnection)
   (java.io
    InputStream
    Reader
    StringReader)
   (java.sql
    Connection)
   (org.postgresql.copy
    CopyManager)
   (org.postgresql.core
    BaseConnection)))

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
                    table (str/join "," columns))
        f* (fn [conn]
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
                                        printer (tc.csv/csv-printer
                                                 sb {:format :postgresql-csv})]
                                    (tc.csv/write-all printer values)
                                    (StringReader. (str sb)))]
                 (copy-in* conn sql reader))))]
    (if (instance? Connection ds)
      (f* ds)
      (with-open [conn (jdbc/get-connection ds)]
        (f* conn)))))
