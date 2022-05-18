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
          :headers {"Content-Disposition" "attachment; filename=\"test.csv\""}}
         (-> (ok "test")
             (attachment "test.csv"))))

  (is (= {:body "test"
          :headers {"Content-Disposition" "attachment; filename=\"%E3%81%82%E3%81%84%E3%81%86%E3%81%88%E3%81%8A.csv\""}
          :status 200}
         (-> (ok "test")
             (attachment "あいうえお.csv"))))

  (is (= {:body "test"
          :headers {"Content-Disposition" "attachment; filename=\"hello world.csv\""}
          :status 200}
         (-> (ok "test")
             (attachment "hello world.csv"))))

  (is (= {:body "test"
          :headers {"Content-Disposition" "attachment; filename=\"hello%2Bworld.csv\""}
          :status 200}
         (-> (ok "test")
             (attachment "hello+world.csv")))))

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

(deftest session-test
  (let [resp (-> (ok "foo")
                 (session {:id 1}))]
    (is (= {:body "foo"
            :status 200
            :headers {}
            :session {:id 1}}
           resp))
    (is (nil? (meta (:session resp)))))

  (let [resp (-> (ok "foo")
                 (session {:id 1} true))]
    (is (= {:body "foo"
            :status 200
            :headers {}
            :session {:id 1}}
           resp))
    (is (= {:recreate true}
           (meta (:session resp))))))

(deftest flash-test
  (is (= {:body "foo"
          :status 200
          :headers {}
          :flash {:id 1}}
         (-> (ok "foo")
             (flash {:id 1})))))

(deftest flash-success-message-test
  (is (= {:body "foo"
          :status 200
          :headers {}
          :flash {:message {:status :success :msg "test message"}}}
         (-> (ok "foo")
             (flash-success-message "test message")))))

(deftest flash-error-message-test
  (is (= {:body "foo"
          :status 200
          :headers {}
          :flash {:message {:status :error :msg "test message"}}}
         (-> (ok "foo")
             (flash-error-message "test message")))))
