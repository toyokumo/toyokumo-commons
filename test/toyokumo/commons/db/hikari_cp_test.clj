(ns toyokumo.commons.db.hikari-cp-test
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [helper :as h]
   [next.jdbc.protocols :as jdbc.proto]
   [toyokumo.commons.db.hikari-cp :refer [map->HikariCP]]
   [toyokumo.commons.health :as health]))

(deftest map->HikariCP-test
  (testing "Not started"
    (let [hc (map->HikariCP {:opts h/test-db-opts})]
      (is (false? (health/alive? hc))
          "alive?")
      (is (nil? (jdbc.proto/get-datasource hc))
          "get-datasource")))

  (testing "started"
    (let [hc (-> (map->HikariCP {:opts h/test-db-opts})
                 (component/start))]
      (is (true? (health/alive? hc))
          "alive?")
      (is (some? (jdbc.proto/get-datasource hc))
          "get-datasource")

      (testing "Then stopped"
        (let [hc (component/stop hc)]
          (is (false? (health/alive? hc))
              "alive?")
          (is (nil? (jdbc.proto/get-datasource hc))
              "get-datasource"))))))
