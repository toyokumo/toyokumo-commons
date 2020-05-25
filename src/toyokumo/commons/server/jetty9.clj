(ns toyokumo.commons.server.jetty9
  (:require
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty9 :as jetty9])
  (:import
   (org.eclipse.jetty.server
    Server)))

(defrecord Jetty9Server [handler opts ^Server server]
  component/Lifecycle
  (start [this]
    (if server
      this
      (assoc this :server (jetty9/run-jetty (:handler handler) opts))))
  (stop [this]
    (when server
      (jetty9/stop-server server))
    (assoc this :server nil)))
