(ns toyokumo.commons.ring.middleware.trailing-slash
  (:require
   [toyokumo.commons.util :as tc.util]))

(defn wrap-trailing-slash
  "Modifies the request :uri and :path-info before calling the handler.
  Removes a single trailing slash from the end of the uri if present.

  Original https://gist.github.com/dannypurcell/8215411"
  [handler]
  (fn [req]
    (-> req
        (update :uri tc.util/remove-trailing-slash)
        (update :path-info tc.util/remove-trailing-slash)
        handler)))
