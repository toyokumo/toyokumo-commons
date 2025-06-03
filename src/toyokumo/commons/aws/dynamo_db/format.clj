(ns toyokumo.commons.aws.dynamo-db.format
  (:require
   [schema.core :as s])
  (:import
   (software.amazon.awssdk.services.dynamodb.model
    AttributeValue)))

(s/defn attribute-value->clj
  "Converts an AttributeValue to a clojure data structure.
   This function supports String, Number, Boolean, and Null types."
  [attribute-value :- AttributeValue]
  (cond
    (some? (.s attribute-value)) (.s attribute-value)
    (some? (.n attribute-value)) (Integer/parseInt (.n attribute-value))
    (some? (.bool attribute-value)) (.bool attribute-value)
    :else nil))
