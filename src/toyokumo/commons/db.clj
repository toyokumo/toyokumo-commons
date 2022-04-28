(ns toyokumo.commons.db
  (:require
   [camel-snake-kebab.core :as csk]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as jdbc.rs]
   [schema.core :as s]))

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

(defn transact-once [tx f opts]
  (if (instance? java.sql.Connection tx)
    (f tx)
    (jdbc/transact tx f opts)))

(defmacro with-db-transaction
  "It is almost same as with-transaction but it supports nested transaction.

  Like clojure.java.jdbc/with-db-transaction when two or more with-db-transaction
  are nested, we want to rollback to top-level with-db-transaction.
  We know we should use {:auto-commit false} and save points but it enables us to
  migrate to next.jdbc easily"
  [[sym transactable opts] & body]
  (let [con (vary-meta sym assoc :tag 'java.sql.Connection)]
    `(transact-once ~transactable (^{:once true} fn* [~con] ~@body) ~(or opts {}))))
