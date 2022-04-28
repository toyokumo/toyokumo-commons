(ns toyokumo.commons.db.postgresql-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest use-fixtures testing is]]
   [helper :as h]
   [toyokumo.commons.csv :as tc.csv]
   [toyokumo.commons.db :as db]
   [toyokumo.commons.db.postgresql :as sut])
  (:import
   (java.io
    File)
   (java.sql
    Timestamp)
   (java.util
    UUID)))

(use-fixtures :each h/test-db-fixture)

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
                 (sut/copy-in @h/hc table columns reader)))
          (is (= values
                 (map #(dissoc % :id)
                      (db/fetch @h/hc ["select * from toyokumo_commons"]))))
          (is (= (count values)
                 (db/execute-batch @h/hc ["delete from toyokumo_commons"])))))
      (testing "InputStream"
        (with-open [stream (io/input-stream csv-file)]
          (is (= (count values)
                 (sut/copy-in @h/hc table columns stream)))
          (is (= values
                 (map #(dissoc % :id)
                      (db/fetch @h/hc ["select * from toyokumo_commons"]))))
          (is (= (count values)
                 (db/execute-batch @h/hc ["delete from toyokumo_commons"])))))
      (testing "String"
        (is (= (count values)
               (sut/copy-in @h/hc table columns (slurp csv-file))))
        (is (= values
               (map #(dissoc % :id)
                    (db/fetch @h/hc ["select * from toyokumo_commons"]))))
        (is (= (count values)
               (db/execute-batch @h/hc ["delete from toyokumo_commons"]))))
      (testing "Sequence"
        (is (= (count values)
               (sut/copy-in @h/hc table columns csv)))
        (is (= values
               (map #(dissoc % :id)
                    (db/fetch @h/hc ["select * from toyokumo_commons"]))))
        (is (= (count values)
               (db/execute-batch @h/hc ["delete from toyokumo_commons"]))))
      (finally
        (.delete csv-file)))))
