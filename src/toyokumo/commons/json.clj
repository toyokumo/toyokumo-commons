(ns toyokumo.commons.json
  (:require
   [camel-snake-kebab.core :as csk]
   [cheshire.core :as json]
   [schema.core :as s]))

(s/defn json-encode :- s/Str
  [x :- s/Any]
  (json/generate-string x {:key-fn csk/->camelCaseString
                           :escape-non-ascii true}))

(s/defn json-decode :- s/Any
  [s :- s/Str]
  (json/parse-string s csk/->kebab-case-keyword))
