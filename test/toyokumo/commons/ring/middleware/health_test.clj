(ns toyokumo.commons.ring.middleware.health-test
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [helper :as h]
   [toyokumo.commons.db.hikari-cp :as tc.hc]
   [toyokumo.commons.redis.carmine :as tc.car]
   [toyokumo.commons.ring.middleware.health :refer [wrap-health-check]]
   [toyokumo.commons.ring.response :as res]))

(deftest wrap-health-check-test
  (let [hc (component/start (tc.hc/map->HikariCP {:opts h/test-db-opts}))
        redis (component/start (tc.car/map->Carmine {:pool {}
                                                     :spec {:host "localhost"}}))
        handler (wrap-health-check identity "/health" hc redis)]
    (testing "health check path"
      (is (= (res/html (res/ok "ok"))
             (handler {:path-info "/health"}))))
    (testing "other path"
      (is (= {:path-info "/foo"}
             (handler {:path-info "/foo"})))
      (is (= {:path-info "/"}
             (handler {:path-info "/"}))))
    (testing "a component is dead"
      (component/stop hc)
      (component/stop redis)
      (is (= (res/html (res/internal-server-error "There is one or more dead component"))
             (handler {:path-info "/health"}))))))
