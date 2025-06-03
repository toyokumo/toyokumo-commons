(ns toyokumo.commons.aws.dynamo-db.request
  (:require
   [schema.core :as s]
   [toyokumo.commons.aws.dynamo-db.format :as format])
  (:import
   (software.amazon.awssdk.services.dynamodb
    DynamoDbClient)
   (software.amazon.awssdk.services.dynamodb.model
    AttributeValue
    GetItemRequest)))

(s/defschema PartitionKeyInfo {:name s/Str
                               :value s/Str})

(s/defschema PartitionKey {s/Str AttributeValue})

(s/defn ^:private create-partition-key :- {s/Str AttributeValue}
  "Creates a partition key map for DynamoDB.
   The key is the partition key name and the value is the attribute value."
  [{:keys [:name :value]} :- PartitionKeyInfo]
  {name (-> (AttributeValue/builder)
            (.s value)
            (.build))})

(s/defschema DynamoDbItem {s/Keyword (s/maybe (s/cond-pre s/Int s/Str s/Bool))})

(s/defn ^:private build-get-item-request
  "Builds a GetItemRequest to retrieve an item from the specified table using the partition key."
  [table-name :- s/Str
   partition-key :- PartitionKey]
  (-> (GetItemRequest/builder)
      (.tableName table-name)
      (.key partition-key)
      (.build)))

(s/defn get-item :- (s/maybe DynamoDbItem)
  "Get item from DynamoDB table using the provided partition key.
   Returns a map of attribute names to AttributeValue, or nil if the item does not exist."
  [client :- DynamoDbClient
   table-name :- s/Str
   partition-key-info :- PartitionKeyInfo]
  (let [partition-key (create-partition-key partition-key-info)
        get-item-res (.getItem client
                               (build-get-item-request table-name
                                                       partition-key))]
    (when (.hasItem get-item-res)
      (-> (.item get-item-res)
          (update-keys keyword)
          (update-vals format/attribute-value->clj)))))
