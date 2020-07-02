(ns toyokumo.commons.core)

(defn coll->map
  "Make a map from a sequence which has keys made by key-fn and values by val-fn"
  [key-fn val-fn coll]
  (reduce (fn [acc m]
            (assoc acc (key-fn m) (val-fn m)))
          {} coll))

(defn qualified-name
  "Like name, returns the name String of a string, symbol or keyword but saves qualified name"
  [x]
  (cond
    (string? x) x
    (keyword? x) (.toString (.sym ^clojure.lang.Keyword x))
    :else (.toString x)))
