(ns toyokumo.commons.transit
  (:require
   [cognitect.transit :as t])
  (:import
   (java.io
    ByteArrayInputStream
    ByteArrayOutputStream)))

(defn transit-encode
  ^String [x & [{:keys [type opts]}]]
  (let [out (ByteArrayOutputStream.)
        type (or type :json)
        opts (or opts {})
        writer (t/writer out type opts)]
    (t/write writer x)
    (str out)))

(defn transit-decode
  [^String s & [{:keys [type opts]}]]
  (let [in (ByteArrayInputStream. (.getBytes s))
        type (or type :json)
        opts (or opts {})
        reader (t/reader in type opts)]
    (t/read reader)))
