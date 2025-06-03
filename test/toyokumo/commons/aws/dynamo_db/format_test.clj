(ns toyokumo.commons.aws.dynamo-db.format-test
  (:require
   [clojure.test :refer [deftest is]]
   [toyokumo.commons.aws.dynamo-db.format :as sut])
  (:import
   (clojure.lang
    ExceptionInfo)
   (software.amazon.awssdk.services.dynamodb.model
    AttributeValue)))

;; (deftest clj-string->ddb-string-test
;;   (is (= "dummy-string"
;;          (-> (sut/clj-string->ddb-string "dummy-string")
;;              (.s)))))

;; (deftest ddb-string->clj-string-test
;;   (is (= "dummy-string"
;;          (sut/ddb-string->clj-string
;;           (sut/clj-string->ddb-string "dummy-string"))))
;;   (is (thrown? ExceptionInfo
;;         (sut/ddb-string->clj-string
;;          (-> (AttributeValue/builder)
;;              (.n "1")
;;              (.build))))
;;       "When passing an AttributeValue that is not of type String, an exception is thrown."))

;; (deftest clj-int->ddb-number-test
;;   (is (= "100"
;;          (-> (sut/clj-int->ddb-number 100)
;;              (.n)))))

;; (deftest ddb-number->clj-int-test
;;   (is (= 100
;;          (sut/ddb-number->clj-int
;;           (sut/clj-int->ddb-number 100)))
;;       (is (thrown? ExceptionInfo
;;             (sut/ddb-number->clj-int
;;              (-> (AttributeValue/builder)
;;                  (.s "dummy-string")
;;                  (.build))))
;;           "When passing an AttributeValue that is not of type Number, an exception is thrown.")))
