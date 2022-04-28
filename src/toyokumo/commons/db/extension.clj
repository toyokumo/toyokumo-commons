(ns toyokumo.commons.db.extension
  "Provides extensions of reading objects from the `java.sql.ResultSet`
  or setting SQL parameters in statement objects.
  See also next.jdbc.date-time, which provides default datetime extensions.

  See for more detail https://github.com/seancorfield/next-jdbc/blob/master/doc/tips-and-tricks.md#postgresql"
  (:require
   [next.jdbc.result-set :as jdbc.rs])
  (:import
   (java.sql
    Array)))

(defn read-array-as-sequence
  "Read array as Clojure sequence"
  []
  (extend-protocol jdbc.rs/ReadableColumn
    Array
    (read-column-by-label [^Array v _]
      (sequence (.getArray v)))
    (read-column-by-index [^Array v _2 _3]
      (sequence (.getArray v)))))
