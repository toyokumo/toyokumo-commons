(ns toyokumo.commons.db.hikari-cp
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [hikari-cp.core :as hc]
   [next.jdbc :as jdbc]
   [next.jdbc.protocols :as jdbc.proto]
   [toyokumo.commons.health :as health])
  (:import
   (javax.sql
    DataSource)))

(defrecord HikariCP [opts ^DataSource datasource]
  component/Lifecycle
  (start [this]
    (if datasource
      this
      (assoc this :datasource (hc/make-datasource opts))))
  (stop [this]
    (when datasource
      (hc/close-datasource datasource))
    (assoc this :datasource nil))

  jdbc.proto/Sourceable
  (get-datasource [_]
    datasource)

  health/HealthCheck
  (-alive? [this]
    (if datasource
      (try
        (= 1 (:alive (jdbc/execute-one! this ["select 1 as alive"])))
        (catch Exception e
          (log/error "HikariCP is dead" e)
          false))
      false)))
