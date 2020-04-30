(ns toyokumo.commons.url-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.url :refer [url-encode url-decode]]))

(deftest url-encode-test
  (is (= "abc123"
         (url-encode "abc123")))
  (is (= "%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A"
         (url-encode "あいうえお"))))

(deftest url-decode-test
  (is (= "abc123"
         (url-decode "abc123")))
  (is (= "あいうえお"
         (url-decode "%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A"))))
