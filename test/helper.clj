(ns helper
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [next.jdbc :as jdbc]
   [schema.core :as s]
   [toyokumo.commons.db.hikari-cp :as tc.hc]))

(defn enable-validation-fixture [f]
  (s/set-fn-validation! true)
  (f))

(def test-db-opts {:adapter "postgresql"
                   :username "toyokumo"
                   :password "commons"
                   :database-name "toyokumo"
                   :port-number 5432})

(defonce hc (atom nil))

(defn test-db-fixture [f]
  (reset! hc (component/start (tc.hc/map->HikariCP {:opts test-db-opts})))
  (jdbc/execute! @hc ["drop table if exists toyokumo_commons"])
  (jdbc/execute-one! @hc ["
    create table toyokumo_commons (
    id bigserial PRIMARY KEY,
    uid uuid NOT NULL,
    product text NOT NULL,
    price int,
    ratio decimal,
    created_at timestamp NOT NULL DEFAULT now()
    )"])
  (try
    (f)
    (catch Exception _))
  (jdbc/execute! @hc ["drop table if exists toyokumo_commons"])
  (component/stop @hc)
  (reset! hc nil))
