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
             (html))))
  (is (= {:body "test"
          :status 200
          :headers {"Content-Type" "text/html; charset=sjis"}}
         (-> (ok "test")
             (html "sjis")))))

(deftest json-test
  (is (= {:body "test"
          :status 200
          :headers {"Content-Type" "application/json"}}
         (-> (ok "test")
             (json))))
  (is (= {:body "test"
          :status 200
          :headers {"Content-Type" "application/json; charset=sjis"}}
         (-> (ok "test")
             (json "sjis")))))

(deftest content-disposition-test
  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" "hoge"}}
         (-> (ok "test")
             (content-disposition "hoge")))))

(deftest attachment-test
  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" "attachment; filename=\"test.csv\"; filename*=UTF-8''test.csv"}}
         (-> (ok "test")
             (attachment "test.csv"))))

  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" (str "attachment; filename=\"あいうえお.csv\"; filename*=UTF-8''"
                                               (tc.url/url-encode "あいうえお.csv"))}}
         (-> (ok "test")
             (attachment "あいうえお.csv"))))

  (is (= {:body "test"
          :status 200
          :headers {"Content-Disposition" (str "attachment; filename=\"あいうえお.csv\"; filename*=windows-31j''"
                                               (tc.url/url-encode "あいうえお.csv" "windows-31j"))}}
         (-> (ok "test")
             (attachment "あいうえお.csv" "windows-31j")))))

(deftest csv-test
  (is (= {:body "foo,bar"
          :status 200
          :headers {"Content-Type" "text/csv"}}
         (-> (ok "foo,bar")
             (csv))))
  (is (= {:body "foo,bar"
          :status 200
          :headers {"Content-Type" "text/csv; charset=sjis"}}
         (-> (ok "foo,bar")
             (csv "sjis")))))
