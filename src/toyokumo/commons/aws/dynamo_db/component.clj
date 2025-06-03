(ns toyokumo.commons.aws.dynamo-db.component
  (:require
   [com.stuartsierra.component :as component]
   [schema.core :as s]
   [toyokumo.commons.aws.dynamo-db.protocol :as tc.ddb.protocol]
   [toyokumo.commons.aws.dynamo-db.request :as tc.ddb.req])
  (:import
   (java.net
    URI)
   (software.amazon.awssdk.auth.credentials
    AwsCredentialsProvider)
   (software.amazon.awssdk.regions
    Region)
   (software.amazon.awssdk.services.dynamodb
    DynamoDbClient)))

(s/defrecord AwsDynamoDb
  [dynamo-db-client :- DynamoDbClient
   region :- (s/maybe Region)
   endpoint :- (s/maybe URI)
   credentials :- (s/maybe AwsCredentialsProvider)]

  component/Lifecycle
  (start [this]
    (let [client (-> (DynamoDbClient/builder)
                     (cond->
                      region (.region region)
                      endpoint (.endpointOverride endpoint)
                      credentials (.credentialsProvider credentials))
                     (.build))]
      (println "DynamoDB client created" client)
      (assoc this
             :client client)))
  (stop [this]
    (when-let [client (:client this)]
      (.close client))
    (assoc this :client nil))

  tc.ddb.protocol/IDynamoDb
  (get-item [this table-name partition-key]
    (tc.ddb.req/get-item (:client this) table-name partition-key)))
