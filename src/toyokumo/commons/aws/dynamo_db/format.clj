(ns toyokumo.commons.aws.dynamo-db.format
  (:require
   [schema.core :as s])
  (:import
   (software.amazon.awssdk.services.dynamodb.model
    AttributeValue)))

;; (s/defn clj-string->ddb-string :- AttributeValue
;;   "This function creates an AttributeValue with type String from a clojure String."
;;   [value :- s/Str]
;;   (-> (AttributeValue/builder)
;;       (.s value)
;;       (.build)))

;; (s/defn ddb-string->clj-string :- s/Str
;;   "This function converts an AttributeValue of type String to a clojure String."
;;   [attribute-value :- AttributeValue]
;;   (if-let [value (.s attribute-value)]
;;     value
;;     (throw (ex-info "AttributeValue is not type String" {}))))


;; (s/defn clj-int->ddb-number :- AttributeValue
;;   "This function creates an AttributeValue with type Number from a clojure Int."
;;   [value :- s/Int]
;;   (-> (AttributeValue/builder)
;;       (.n (str value))
;;       (.build)))

;; (s/defn ddb-number->clj-int :- s/Int
;;   "This function converts an AttributeValue of type Number to a clojure Int."
;;   [attribute-value :- AttributeValue]
;;   (if-let [value (.n attribute-value)]
;;     (-> value
;;         (Integer/parseInt))
;;     (throw (ex-info "AttributeValue is not type Number" {}))))


(s/defn attribute-value->clj
  "Converts an AttributeValue to a clojure data structure.
   This function supports String, Number, Boolean, and Null types."
  [attribute-value :- AttributeValue]
  (cond
    (some? (.s attribute-value)) (.s attribute-value)
    (some? (.n attribute-value)) (Integer/parseInt (.n attribute-value))
    (some? (.bool attribute-value)) (.bool attribute-value)
    :else nil))
