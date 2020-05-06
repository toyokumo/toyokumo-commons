(ns toyokumo.commons.core-test
  (:require
   [clojure.test :refer :all]
   [helper :as h]
   [toyokumo.commons.core :refer [coll->map]])
  (:import
   (clojure.lang
    ExceptionInfo)))

(use-fixtures :once h/enable-validation-fixture)

(deftest coll->map-test
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
         (coll->map :id identity [])
         (coll->map :id identity #{}))))
