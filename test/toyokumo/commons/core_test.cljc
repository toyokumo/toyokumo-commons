(ns toyokumo.commons.core-test
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]
   [toyokumo.commons.core :refer [coll->map]])
  (:import
   (clojure.lang
    ExceptionInfo)))

(use-fixtures :once (fn [f]
                      (s/set-fn-validation! true)
                      (f)))

(deftest coll->map-test
  (testing "Check arguments"
    (is (thrown? ExceptionInfo (coll->map "foo" identity [])))
    (is (thrown? ExceptionInfo (coll->map identity "foo" []))))

  (testing "Positive case"
    (is (= {1 "foo"
            2 "bar"
            3 "baz"}
           (coll->map :id :name [{:id 1 :name "foo"}
                                 {:id 2 :name "bar"}
                                 {:id 3 :name "baz"}])))
    (is (= {1 {:id 1 :name "foo"}
            2 {:id 2 :name "bar"}
            3 {:id 3 :name "baz"}}
           (coll->map :id identity [{:id 1 :name "foo"}
                                    {:id 2 :name "bar"}
                                    {:id 3 :name "baz"}])))
    (is (= {}
           (coll->map :id identity [])))))
