(ns toyokumo.commons.transit
  (:require
   [cognitect.transit :as t]))

(defn transit-encode
  [x & [{:keys [type opts]}]]
  (let [type (or type :json)
        opts (or opts {})
        writer (t/writer type opts)]
    (t/write writer x)))

(defn transit-decode
  [x & [{:keys [type opts]}]]
  (let [type (or type :json)
        opts (or opts {})
        reader (t/reader type opts)]
    (t/read reader x)))
