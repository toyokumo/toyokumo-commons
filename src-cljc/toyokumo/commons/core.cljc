(ns toyokumo.commons.core
  (:require
   [schema.core :as s])
  (:import
   (clojure.lang
    IFn)))

(s/defn coll->map
  "Make a map from a sequence which has keys made by key-fn and values by val-fn"
  [key-fn :- IFn
   val-fn :- IFn
   coll]
  (reduce (fn [acc m]
            (assoc acc (key-fn m) (val-fn m)))
          {} coll))
