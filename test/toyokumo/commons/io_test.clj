(ns toyokumo.commons.io-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [toyokumo.commons.io :refer [excluding-bom-reader]])
  (:import
   (java.io
    BufferedReader)))

(deftest excluding-bom-reader-test
  (testing "Read utf-8 file"
    (is (= "1,2,3,4,5"
           (with-open [^BufferedReader reader (-> (io/resource "test/utf8.csv")
                                                  (excluding-bom-reader))]
             (.readLine reader)))))
  (testing "Read utf-8 with bom file"
    (is (= "1,2,3,4,5"
           (with-open [^BufferedReader reader (-> (io/resource "test/utf8_bom.csv")
                                                  (excluding-bom-reader :encoding "utf-8"))]
             (.readLine reader)))))
  (testing "Read Shift_JIS file"
    (is (= "1,2,3,4,5"
           (with-open [^BufferedReader reader (-> (io/resource "test/sjis.csv")
                                                  (excluding-bom-reader :encoding "sjis"))]
             (.readLine reader))))))
