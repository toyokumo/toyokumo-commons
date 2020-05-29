(ns toyokumo.commons.ring.middleware.trailing-slash
  (:require
   [clojure.string :as str]))

(defn- ignore-trailing-slash [^String uri]
  (if (and (string? uri)
           (not= uri "/")
           (str/ends-with? uri "/"))
    (subs uri 0 (dec (count uri)))
    uri))

(defn wrap-trailing-slash
  "Modifies the request :uri and :path-info before calling the handler.
  Removes a single trailing slash from the end of the uri if present.

  Original https://gist.github.com/dannypurcell/8215411"
  [handler]
  (fn [req]
    (-> req
        (update :uri ignore-trailing-slash)
        (update :path-info ignore-trailing-slash)
        handler)))
