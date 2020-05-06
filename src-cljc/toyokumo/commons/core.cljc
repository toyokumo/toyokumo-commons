(ns toyokumo.commons.core)

(defn coll->map
  "Make a map from a sequence which has keys made by key-fn and values by val-fn"
  [key-fn val-fn coll]
  (reduce (fn [acc m]
            (assoc acc (key-fn m) (val-fn m)))
          {} coll))
