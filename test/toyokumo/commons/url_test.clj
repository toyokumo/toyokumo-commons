(ns toyokumo.commons.url-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.url :refer [url-encode url-decode]])
  (:import
   (java.io
    UnsupportedEncodingException)))

(deftest url-encode-test
  (is (= "abc123"
         (url-encode "abc123")))
  (is (= "%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A"
         (url-encode "あいうえお")
         (url-encode "あいうえお" "utf-8")))
  (is (= "%82%A0%82%A2%82%A4%82%A6%82%A8"
         (url-encode "あいうえお" "sjis")
         (url-encode "あいうえお" "Windows-31j")))
  (is (thrown? UnsupportedEncodingException (url-encode "あいうえお" "hoge"))))

(deftest url-decode-test
  (is (= "abc123"
         (url-decode "abc123")))
  (is (= "あいうえお"
         (url-decode "%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A")
         (url-decode "%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A" "utf-8")))
  (is (= "あいうえお"
         (url-decode "%82%A0%82%A2%82%A4%82%A6%82%A8" "sjis")
         (url-decode "%82%A0%82%A2%82%A4%82%A6%82%A8" "Windows-31j")))
  (is (thrown? UnsupportedEncodingException (url-decode "%82%A0%82%A2%82%A4%82%A6%82%A8" "hoge"))))
