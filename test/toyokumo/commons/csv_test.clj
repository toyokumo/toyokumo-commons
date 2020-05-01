(ns toyokumo.commons.csv-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [toyokumo.commons.csv :refer :all])
  (:import
   (java.io
    File)
   (org.apache.commons.io.input
    BOMInputStream)))

(def test-contents
  [["1" "2" "3" "4" "5"]
   ["a" "b" "c" "d" "e"]
   ["あ" "い" "う" "え" "お"]])

(deftest read-all-test
  (testing "Read utf-8 file"
    (is (= test-contents
           (with-open [parser (-> (io/resource "test/utf8.csv")
                                  (io/reader :encoding "utf-8")
                                  (csv-parser {:format :rfc4180}))]
             (read-all parser)))))
  (testing "Read utf-8 with bom file"
    (is (= test-contents
           (with-open [parser (-> (io/resource "test/utf8_bom.csv")
                                  (io/input-stream :encoding "utf-8")
                                  (BOMInputStream.)
                                  (io/reader)
                                  (csv-parser {:format :rfc4180}))]
             (read-all parser)))))
  (testing "Read Shift_JIS file"
    (is (= test-contents
           (with-open [parser (-> (io/resource "test/sjis.csv")
                                  (io/reader :encoding "sjis")
                                  (csv-parser {:format :rfc4180}))]
             (read-all parser))))))

(deftest write-all-test
  (testing "Write on memory test"
    (is (= (str (str/join "\r\n" (map #(str/join "," %) test-contents))
                "\r\n")
           (let [sb (StringBuilder.)
                 printer (csv-printer sb {:format :rfc4180})]
             (write-all printer test-contents)
             (str sb)))))

  (testing "Write to a file test"
    (let [f (File/createTempFile "test" ".csv")]
      (try
        (with-open [printer (-> f
                                (io/writer :encoding "utf-8")
                                (csv-printer {:format :rfc4180}))]
          (write-all printer test-contents))
        (is (= (str (str/join "\r\n" (map #(str/join "," %) test-contents))
                    "\r\n")
               (slurp f)))
        (finally
          (.delete f))))))
