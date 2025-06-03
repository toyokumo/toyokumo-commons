(ns toyokumo.commons.aws.dynamo-db.protocol)

(defprotocol IDynamoDb
  (get-item [this table-name partition-key]) ; Get item from DynamoDB table using the provided partition key
  )
