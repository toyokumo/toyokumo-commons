(ns toyokumo.commons.redis.carmine-test
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [toyokumo.commons.health :as health]
   [toyokumo.commons.redis.carmine :refer [map->Carmine]]))

(deftest map->Carmine-test
  (testing "alive?"
    (testing "incorrect spec"
      (let [c (map->Carmine {:pool {}
                             :spec {:host "localhost"
                                    :port 3333}})]
        (is (false? (health/alive? c)))))
    (testing "correct spec"
      (let [c (map->Carmine {:pool {}
                             :spec {:host "localhost"
                                    :port 6379}})]
        (is (true? (health/alive? c))))))
  (testing "Lifecycle"
    (let [c (component/start
             (map->Carmine {:pool {}
                            :spec {:host "localhost"
                                   :port 6379}}))]
      (is (true? (health/alive? c)))
      (is (= (map->Carmine {:pool nil :spec nil})
             (component/stop c))))))
