(ns toyokumo.commons.ring.response-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.ring.response :refer :all]
   [toyokumo.commons.url :as tc.url]))

(deftest html-test
  (is (= {:body "test"
          :status 200
          :headers {"Content-Type" "text/html"}}
         (-> (ok "test")
             (html)))))

(deftest json-test
  (is (= {:body "test"
          :status 200
          :headers {"Content-Type" "application/json"}}
         (-> (ok "test")
             (json)))))

(deftest content-disposition-test
  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" "hoge"}}
         (-> (ok "test")
             (content-disposition "hoge")))))

(deftest csv-test
  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" "attachment; filename=\"test.csv\"; filename*=UTF-8''test.csv"
                    "Content-Type" "text/csv; charset=UTF-8"}}
         (-> (ok "test")
             (csv "test.csv"))))

  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" (str "attachment; filename=\"あいうえお.csv\"; filename*=UTF-8''"
                                               (tc.url/url-encode "あいうえお.csv"))
                    "Content-Type" "text/csv; charset=UTF-8"}}
         (-> (ok "test")
             (csv "あいうえお.csv"))))

  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" (str "attachment; filename=\"あいうえお.csv\"; filename*=windows-31j''"
                                               (tc.url/url-encode "あいうえお.csv" "windows-31j"))
                    "Content-Type" "text/csv; charset=windows-31j"}}
         (-> (ok "test")
             (csv "あいうえお.csv" "windows-31j")))))
