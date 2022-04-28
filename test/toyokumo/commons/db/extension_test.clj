(ns toyokumo.commons.db.extension-test
  (:require
   [clojure.test :refer :all]
   [helper :as h]
   [toyokumo.commons.db :as db]
   [toyokumo.commons.db.extension :as ext])
  (:import
   (java.sql
    Timestamp)
   (java.util
    UUID)))

(use-fixtures :each h/test-db-fixture)

(deftest read-array-as-sequence-test
  (ext/read-array-as-sequence)
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
        at3 (Timestamp/valueOf "2020-05-22 17:18:33")]
    (db/execute-batch @h/hc
                      ["insert into toyokumo_commons
                      (uid, product, price, ratio, created_at)
                      values
                      (?, ?, ?, ?, ?),
                      (?, ?, ?, ?, ?),
                      (?, ?, ?, ?, ?)"
                       uid1 product1 price1 ratio1 at1
                       uid2 product2 price2 ratio2 at2
                       uid3 product3 price3 ratio3 at3])
    (is (= {:uuids [uid1 uid2 uid3]
            :products [product1 product2 product3]
            :prices [price1 price2 price3]
            :ratios (map bigdec [ratio1 ratio2 ratio3])
            :ats [at1 at2 at3]}
           (db/fetch-one @h/hc [(str "select"
                                     " array_agg(uid) as uuids"
                                     " ,array_agg(product) as products"
                                     " ,array_agg(price) as prices"
                                     " ,array_agg(ratio) as ratios"
                                     " ,array_agg(created_at) as ats"
                                     " from toyokumo_commons")])))))
