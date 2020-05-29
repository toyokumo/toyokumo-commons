(ns toyokumo.commons.ring.middleware.health
  (:require
   [clojure.tools.logging :as log]
   [ring.util.request :as u.req]
   [toyokumo.commons.health :as tc.health]
   [toyokumo.commons.ring.response :as res]))

(defn- health-check [components]
  (cond
    (empty? components)
    true

    (some nil? components)
    (throw (IllegalArgumentException. "component must not be nil"))

    :else
    (try
      (loop [[c & restc] components]
        (if-not c
          true
          (if (tc.health/alive? c)
            (recur restc)
            false)))
      (catch Exception e
        (log/error "A component throws exception" e)
        false))))

(defn wrap-health-check
  "If a request comes to health-check-path then capture it, return response and
  check whether components are alive"
  [handler health-check-path & components]
  (fn [req]
    (if (= health-check-path (u.req/path-info req))
      (if (health-check components)
        (res/html (res/ok "ok"))
        (res/html (res/internal-server-error "There is one or more dead component")))
      (handler req))))
