(ns toyokumo.commons.db.extension.postgresql-test
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.test :refer [use-fixtures deftest testing is]]
   [helper :as h]
   [jsonista.core :as json]
   [toyokumo.commons.db :as db]
   [toyokumo.commons.db.extension.postgresql :as sut])
  (:import
   (java.util
    UUID)))

(use-fixtures :each h/test-db-fixture)

(deftest json-read-write-test
  (testing "default mapper"
    (sut/set-json-as-parameter)
    (sut/read-json)
    (let [v {:id 1
             :uid (UUID/randomUUID)
             :foo-bar :bar
             :arr [1 2 3]}]
      (is (= {:my-json (-> v
                           (update :uid str)
                           (update :foo-bar name))}
             (db/execute-one @h/hc
                             ["insert into json_test (my_json) values (?)"
                              v])))
      (is (= [{:my-json (-> v
                            (update :uid str)
                            (update :foo-bar name))}]
             (db/fetch @h/hc
                       ["select * from json_test"])))))

  (testing "custom mapper"
    (sut/set-json-as-parameter)
    (sut/read-json (json/object-mapper {:decode-key-fn csk/->snake_case_keyword}))
    (let [v {:id 2
             :uid (UUID/randomUUID)
             :foo-bar :baz
             :arr [1.1 2.2 3.3]}]
      (is (= {:my-json (-> v
                           (update :uid str)
                           (dissoc :foo-bar)
                           (assoc :foo_bar "baz"))}
             (db/execute-one @h/hc
                             ["insert into json_test (my_json) values (?)"
                              v]))))))
