(ns toyokumo.commons.url
  (:require
   [schema.core :as s])
  (:import
   (org.apache.commons.codec.net
    URLCodec)))

(def ^:dynamic url-codec (URLCodec. "UTF-8"))

(s/defn url-encode :- s/Str
  [s :- s/Str]
  (.encode ^URLCodec url-codec s))

(s/defn url-decode :- s/Str
  [s :- s/Str]
  (.decode ^URLCodec url-codec s))
