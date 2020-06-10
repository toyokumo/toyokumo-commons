(ns toyokumo.commons.util
  (:require
   [clojure.string :as str]))

(defn remove-trailing-slash
  ^String [^String uri]
  (if (and (string? uri)
           (not= uri "/")
           (str/ends-with? uri "/"))
    (subs uri 0 (dec (count uri)))
    uri))
