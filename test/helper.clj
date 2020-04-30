(ns helper
  (:require
   [clojure.test :refer :all]
   [schema.core :as s]))

(defn enable-validation-fixture [f]
  (s/set-fn-validation! true)
  (f))
