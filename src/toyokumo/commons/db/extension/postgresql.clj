(ns ^:no-doc toyokumo.commons.db.extension.postgresql
  "Provides extensions of reading objects that are in PostgreSQL from the `java.sql.ResultSet`
  or setting SQL parameters in statement objects.
  See also next.jdbc.date-time, which provides default datetime extensions."
  (:require
   [jsonista.core :as json]
   [next.jdbc.prepare :as jdbc.pre]
   [next.jdbc.result-set :as jdbc.rs])
  (:import
   (clojure.lang
    IPersistentMap)
   (java.sql
    PreparedStatement)
   (org.postgresql.util
    PGobject)))

(defn set-json-as-parameter
  "Make Clojure map jsonb in PreparedStatement
  See https://github.com/seancorfield/next-jdbc/blob/master/doc/tips-and-tricks.md#working-with-json-and-jsonb"
  ([]
   (set-json-as-parameter nil))
  ([mapper]
   (let [mapper (or mapper (json/object-mapper))
         ->json (fn [v]
                  (json/write-value-as-string v mapper))
         ->pgobject (fn [x]
                      (let [pgtype (or (:pgtype (meta x)) "jsonb")]
                        (doto (PGobject.)
                          (.setType pgtype)
                          (.setValue (->json x)))))]
     (extend-protocol jdbc.pre/SettableParameter
       IPersistentMap
       (set-parameter [m ^PreparedStatement s i]
         (.setObject s i (->pgobject m)))))))

(defn read-json
  "Read jsonb or json as Clojure data
  See https://github.com/seancorfield/next-jdbc/blob/master/doc/tips-and-tricks.md#working-with-json-and-jsonb"
  ([]
   (read-json nil))
  ([mapper]
   (let [mapper (or mapper (json/object-mapper {:decode-key-fn keyword}))
         <-json (fn [v]
                  (json/read-value v mapper))
         <-pgobject (fn [v]
                      (let [type (.getType v)
                            value (.getValue v)]
                        (if (#{"jsonb" "json"} type)
                          (with-meta (<-json value) {:pgtype type})
                          value)))]
     (extend-protocol jdbc.rs/ReadableColumn
       PGobject
       (read-column-by-label [^PGobject v _]
         (<-pgobject v))
       (read-column-by-index [^PGobject v _2 _3]
         (<-pgobject v))))))
