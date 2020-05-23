(ns toyokumo.commons.db-test
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [helper :as h]
   [toyokumo.commons.csv :as tc.csv]
   [toyokumo.commons.db :as db])
  (:import
   (java.io
    File)
   (java.sql
    Timestamp)
   (java.util
    UUID)))

(use-fixtures :each h/test-db-fixture)

(deftest default-work-tests
  (let [uid1 (UUID/randomUUID)
        product1 "foo"
        price1 10000
        ratio1 0.25
        at1 (Timestamp/valueOf "2020-05-22 16:35:04")

        uid2 (UUID/randomUUID)
        product2 "bar"
        price2 19800
        ratio2 0.372
        at2 (Timestamp/valueOf "2020-05-22 16:35:04")

        uid3 (UUID/randomUUID)
        product3 "baz"
        price3 39800
        ratio3 0.872
        at3 (Timestamp/valueOf "2020-05-22 17:18:33")

        uid4 (UUID/randomUUID)
        product4 "hoge"
        price4 100
        ratio4 0.01
        at4 (Timestamp/valueOf "2020-05-22 17:21:12")

        uid5 (UUID/randomUUID)
        product5 "fuga"
        price5 200
        ratio5 0.001
        at5 (Timestamp/valueOf "2020-05-22 17:21:12")

        v1 {:uid uid1
            :product product1
            :price price1
            :ratio (bigdec ratio1)
            :created-at at1}
        v2 {:uid uid2
            :product product2
            :price price2
            :ratio (bigdec ratio2)
            :created-at at2}
        v3 {:uid uid3
            :product product3
            :price price3
            :ratio (bigdec ratio3)
            :created-at at3}
        v4 {:uid uid4
            :product product4
            :price price4
            :ratio (bigdec ratio4)
            :created-at at4}
        v5 {:uid uid5
            :product product5
            :price price5
            :ratio (bigdec ratio5)
            :created-at at5}]
    (testing "insert"
      (testing "execute"
        (is (= [(assoc v1 :id 1)
                (assoc v2 :id 2)]
               (db/execute @h/hc
                           ["insert into toyokumo_commons
                           (uid, product, price, ratio, created_at)
                           values
                           (?, ?, ?, ?, ?),
                           (?, ?, ?, ?, ?)"
                            uid1 product1 price1 ratio1 at1
                            uid2 product2 price2 ratio2 at2]))))
      (testing "execute-one"
        (is (= (assoc v3 :id 3)
               (db/execute-one @h/hc
                               ["insert into toyokumo_commons
                               (uid, product, price, ratio, created_at)
                               values
                               (?, ?, ?, ?, ?)"
                                uid3 product3 price3 ratio3 at3]))))
      (testing "execute-batch"
        (is (= 2
               (db/execute-batch @h/hc
                                 ["insert into toyokumo_commons
                                 (uid, product, price, ratio, created_at)
                                 values
                                 (?, ?, ?, ?, ?),
                                 (?, ?, ?, ?, ?)"
                                  uid4 product4 price4 ratio4 at4
                                  uid5 product5 price5 ratio5 at5])))))
    (testing "select"
      (testing "fetch"
        (is (= []
               (db/fetch @h/hc
                         ["select * from toyokumo_commons where id = ?"
                          Long/MAX_VALUE])))
        (is (= [(assoc v4 :id 4)
                (assoc v5 :id 5)]
               (db/fetch @h/hc
                         ["select * from toyokumo_commons where id in (4,5) order by id"])))
        (is (= [(assoc v3 :id 3)]
               (db/fetch @h/hc
                         ["select * from toyokumo_commons
                         where product = ?"
                          "baz"]))))
      (testing "fetch-one"
        (is (= nil
               (db/fetch-one @h/hc
                             ["select * from toyokumo_commons where id = ?"
                              Long/MAX_VALUE])))
        (is (= (assoc v1 :id 1)
               (db/fetch-one @h/hc
                             ["select * from toyokumo_commons where uid = ?"
                              uid1])))
        (is (= (assoc v3 :id 3)
               (db/fetch-one @h/hc
                             ["select * from toyokumo_commons
                             where product = ?"
                              "baz"])))))
    (testing "Update"
      (testing "execute"
        (is (= [(assoc v1 :id 1 :ratio 1M)
                (assoc v2 :id 2 :ratio 1M)]
               (db/execute @h/hc
                           ["update toyokumo_commons
                           set ratio = ?
                           where uid in (?, ?)"
                            1.00 uid1 uid2]))))
      (testing "execute-one"
        (is (= (assoc v1 :id 1 :ratio (bigdec ratio1))
               (db/execute-one @h/hc
                               ["update toyokumo_commons
                               set ratio = ?
                               where uid = ?"
                                ratio1 uid1]))))
      (testing "execute-batch"
        (is (= 2
               (db/execute-batch @h/hc
                                 ["update toyokumo_commons
                                 set ratio = ?
                                 where uid in (?, ?)"
                                  1.00 uid1 uid2])))))
    (testing "Delete"
      (testing "execute"
        (is (= [(assoc v4 :id 4)
                (assoc v5 :id 5)]
               (db/execute @h/hc
                           ["delete from toyokumo_commons
                           where uid in (?, ?)"
                            uid4 uid5]))))
      (testing "execute-one"
        (is (= (assoc v3 :id 3)
               (db/execute-one @h/hc
                               ["delete from toyokumo_commons
                               where product = ?"
                                product3]))))
      (testing "execute-batch"
        (is (= 1
               (db/execute-batch @h/hc
                                 ["delete from toyokumo_commons
                                 where uid in (?, ?)"
                                  uid2 (UUID/randomUUID)])))))))

(deftest default-work-tests
  ;; Change label-fn !
  (alter-var-root #'toyokumo.commons.db/*label-fn*
                  (fn [_] csk/->snake_case))
  (let [uid1 (UUID/randomUUID)
        product1 "foo"
        price1 10000
        ratio1 0.25
        at1 (Timestamp/valueOf "2020-05-22 16:35:04")

        uid2 (UUID/randomUUID)
        product2 "bar"
        price2 19800
        ratio2 0.372
        at2 (Timestamp/valueOf "2020-05-22 16:35:04")

        uid3 (UUID/randomUUID)
        product3 "baz"
        price3 39800
        ratio3 0.872
        at3 (Timestamp/valueOf "2020-05-22 17:18:33")

        uid4 (UUID/randomUUID)
        product4 "hoge"
        price4 100
        ratio4 0.01
        at4 (Timestamp/valueOf "2020-05-22 17:21:12")

        uid5 (UUID/randomUUID)
        product5 "fuga"
        price5 200
        ratio5 0.001
        at5 (Timestamp/valueOf "2020-05-22 17:21:12")

        v1 {:uid uid1
            :product product1
            :price price1
            :ratio (bigdec ratio1)
            :created_at at1}
        v2 {:uid uid2
            :product product2
            :price price2
            :ratio (bigdec ratio2)
            :created_at at2}
        v3 {:uid uid3
            :product product3
            :price price3
            :ratio (bigdec ratio3)
            :created_at at3}
        v4 {:uid uid4
            :product product4
            :price price4
            :ratio (bigdec ratio4)
            :created_at at4}
        v5 {:uid uid5
            :product product5
            :price price5
            :ratio (bigdec ratio5)
            :created_at at5}]
    (testing "insert"
      (testing "execute"
        (is (= [(assoc v1 :id 1)
                (assoc v2 :id 2)]
               (db/execute @h/hc
                           ["insert into toyokumo_commons
                           (uid, product, price, ratio, created_at)
                           values
                           (?, ?, ?, ?, ?),
                           (?, ?, ?, ?, ?)"
                            uid1 product1 price1 ratio1 at1
                            uid2 product2 price2 ratio2 at2]))))
      (testing "execute-one"
        (is (= (assoc v3 :id 3)
               (db/execute-one @h/hc
                               ["insert into toyokumo_commons
                               (uid, product, price, ratio, created_at)
                               values
                               (?, ?, ?, ?, ?)"
                                uid3 product3 price3 ratio3 at3]))))
      (testing "execute-batch"
        (is (= 2
               (db/execute-batch @h/hc
                                 ["insert into toyokumo_commons
                                 (uid, product, price, ratio, created_at)
                                 values
                                 (?, ?, ?, ?, ?),
                                 (?, ?, ?, ?, ?)"
                                  uid4 product4 price4 ratio4 at4
                                  uid5 product5 price5 ratio5 at5])))))
    (testing "select"
      (testing "fetch"
        (is (= []
               (db/fetch @h/hc
                         ["select * from toyokumo_commons where id = ?"
                          Long/MAX_VALUE])))
        (is (= [(assoc v4 :id 4)
                (assoc v5 :id 5)]
               (db/fetch @h/hc
                         ["select * from toyokumo_commons where id in (4,5) order by id"])))
        (is (= [(assoc v3 :id 3)]
               (db/fetch @h/hc
                         ["select * from toyokumo_commons
                         where product = ?"
                          "baz"]))))
      (testing "fetch-one"
        (is (= nil
               (db/fetch-one @h/hc
                             ["select * from toyokumo_commons where id = ?"
                              Long/MAX_VALUE])))
        (is (= (assoc v1 :id 1)
               (db/fetch-one @h/hc
                             ["select * from toyokumo_commons where uid = ?"
                              uid1])))
        (is (= (assoc v3 :id 3)
               (db/fetch-one @h/hc
                             ["select * from toyokumo_commons
                             where product = ?"
                              "baz"]))))))
  (alter-var-root #'toyokumo.commons.db/*label-fn*
                  (fn [_] csk/->kebab-case)))

(deftest copy-in-test
  (let [uid1 (UUID/randomUUID)
        product1 "foo"
        price1 10000
        ratio1 0.25
        at1 (Timestamp/valueOf "2020-05-22 16:35:04")

        uid2 (UUID/randomUUID)
        product2 "bar"
        price2 19800
        ratio2 0.372
        at2 (Timestamp/valueOf "2020-05-22 16:35:04")

        uid3 (UUID/randomUUID)
        product3 "baz"
        price3 39800
        ratio3 0.872
        at3 (Timestamp/valueOf "2020-05-22 17:18:33")

        uid4 (UUID/randomUUID)
        product4 "hoge"
        price4 100
        ratio4 0.01
        at4 (Timestamp/valueOf "2020-05-22 17:21:12")

        uid5 (UUID/randomUUID)
        product5 "fuga"
        price5 200
        ratio5 0.001
        at5 (Timestamp/valueOf "2020-05-22 17:21:12")

        v1 {:uid uid1
            :product product1
            :price price1
            :ratio (bigdec ratio1)
            :created-at at1}
        v2 {:uid uid2
            :product product2
            :price price2
            :ratio (bigdec ratio2)
            :created-at at2}
        v3 {:uid uid3
            :product product3
            :price price3
            :ratio (bigdec ratio3)
            :created-at at3}
        v4 {:uid uid4
            :product product4
            :price price4
            :ratio (bigdec ratio4)
            :created-at at4}
        v5 {:uid uid5
            :product product5
            :price price5
            :ratio (bigdec ratio5)
            :created-at at5}

        table :toyokumo-commons
        columns [:uid :product :price :ratio :created-at]
        values [v1 v2 v3 v4 v5]
        csv (map (apply juxt columns) values)
        ^File csv-file (File/createTempFile "tmp" ".csv")]
    (try
      (with-open [writer (io/writer csv-file)]
        (-> (tc.csv/csv-printer writer {:format :postgresql-csv})
            (tc.csv/write-all csv)))
      (testing "Reader"
        (with-open [reader (io/reader csv-file)]
          (is (= (count values)
                 (db/copy-in @h/hc table columns reader)))
          (is (= values
                 (map #(dissoc % :id)
                      (db/fetch @h/hc ["select * from toyokumo_commons"]))))
          (is (= (count values)
                 (db/execute-batch @h/hc ["delete from toyokumo_commons"])))))
      (testing "InputStream"
        (with-open [stream (io/input-stream csv-file)]
          (is (= (count values)
                 (db/copy-in @h/hc table columns stream)))
          (is (= values
                 (map #(dissoc % :id)
                      (db/fetch @h/hc ["select * from toyokumo_commons"]))))
          (is (= (count values)
                 (db/execute-batch @h/hc ["delete from toyokumo_commons"])))))
      (testing "String"
        (is (= (count values)
               (db/copy-in @h/hc table columns (slurp csv-file))))
        (is (= values
               (map #(dissoc % :id)
                    (db/fetch @h/hc ["select * from toyokumo_commons"]))))
        (is (= (count values)
               (db/execute-batch @h/hc ["delete from toyokumo_commons"]))))
      (testing "Sequence"
        (is (= (count values)
               (db/copy-in @h/hc table columns csv)))
        (is (= values
               (map #(dissoc % :id)
                    (db/fetch @h/hc ["select * from toyokumo_commons"]))))
        (is (= (count values)
               (db/execute-batch @h/hc ["delete from toyokumo_commons"]))))
      (finally
        (.delete csv-file)))))
