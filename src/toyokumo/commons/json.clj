(ns toyokumo.commons.json
  (:require
   [camel-snake-kebab.core :as csk]
   [jsonista.core :as json]
   [schema.core :as s])
  (:import
   (com.fasterxml.jackson.databind
    ObjectMapper)))

(def ^:dynamic *mapper*
  (json/object-mapper {:escape-non-ascii true
                       :encode-key-fn csk/->camelCaseString
                       :decode-key-fn csk/->kebab-case-keyword}))

(s/defn json-encode :- s/Str
  ([x :- s/Any]
   (json-encode *mapper* x))
  ([mapper :- ObjectMapper
    x :- s/Any]
   (json/write-value-as-string x mapper)))

(s/defn json-decode :- s/Any
  ([s :- s/Str]
   (json-decode *mapper* s))
  ([mapper :- ObjectMapper
    s :- s/Str]
   (json/read-value s mapper)))
