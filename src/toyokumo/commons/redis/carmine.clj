(ns toyokumo.commons.redis.carmine
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [taoensso.carmine :as car]
   [toyokumo.commons.health :as health]))

(defrecord Carmine [pool spec]
  component/Lifecycle
  (start [this] this)
  (stop [this]
    (assoc this
           :pool nil
           :spec nil))

  health/HealthCheck
  (-alive? [this]
    (try
      (some? (car/wcar this (car/ping)))
      (catch Exception e
        (log/error "Carmine is dead" e)
        false))))
