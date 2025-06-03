(ns toyokumo.commons.aws.dynamo-db.format-test
  (:require
   [clojure.test :refer [deftest is]]
   [toyokumo.commons.aws.dynamo-db.format :as attribute-value->clj])
  (:import
   (software.amazon.awssdk.services.dynamodb.model
    AttributeValue)))

(deftest attribute-value->clj-test
  (is (= "dummy-string"
         (-> (AttributeValue/builder)
             (.s "dummy-string")
             (.build)
             attribute-value->clj/attribute-value->clj))
      "Converts an AttributeValue of type String to a Clojure string.")
  (is (= 100
         (-> (AttributeValue/builder)
             (.n "100")
             (.build)
             attribute-value->clj/attribute-value->clj))
      "Converts an AttributeValue of type Number to a Clojure integer.")
  (is (= true
         (-> (AttributeValue/builder)
             (.bool true)
             (.build)
             attribute-value->clj/attribute-value->clj))
      "Converts an AttributeValue of type Boolean to a Clojure boolean."))
