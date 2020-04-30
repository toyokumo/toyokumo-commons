(ns toyokumo.commons.transit
  (:require
   [cognitect.transit :as t]
   [schema.core :as s])
  (:import
   (java.io
    ByteArrayInputStream
    ByteArrayOutputStream)))

(s/defn transit-encode :- s/Str
  [x :- s/Any]
  (let [out (ByteArrayOutputStream.)
        writer (t/writer out :json)]
    (t/write writer x)
    (str out)))

(s/defn transit-decode :- s/Any
  [s :- s/Str]
  (let [in (ByteArrayInputStream. (.getBytes s))
        reader (t/reader in :json)]
    (t/read reader)))
