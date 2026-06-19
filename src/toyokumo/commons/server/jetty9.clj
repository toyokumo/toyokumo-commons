(ns toyokumo.commons.server.jetty9
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty9 :as jetty9])
  (:import
   (java.util.concurrent
    CompletableFuture)
   (org.eclipse.jetty.server
    Server)
   (org.eclipse.jetty.server.handler
    GracefulHandler)
   (org.eclipse.jetty.util.component
    LifeCycle$Listener)))

(defn- logging-graceful-handler
  "Builds a GracefulHandler that logs the start and end of the shutdown wait.
  If the wait does not finish within the stop timeout, the future is never
  completed, so the end log is not emitted (only the start log and the
  server-stopped log appear)."
  []
  (proxy [GracefulHandler] []
    (shutdown []
      (log/infof "jetty graceful shutdown start. in-flight requests:%d"
                 (.getCurrentRequestCount ^GracefulHandler this))
      ;; whenComplete returns a new derived future, so we register the logging
      ;; callback only and return the original future from shutdown. This keeps
      ;; exceptions in the logging callback from affecting Jetty's shutdown
      ;; completion check, and preserves shutdown idempotency (the same future).
      (let [^CompletableFuture fut (proxy-super shutdown)]
        (.whenComplete fut
                       (fn [_ ex]
                         (if ex
                           (log/warn ex "jetty graceful shutdown aborted")
                           (log/info "jetty graceful shutdown end. all in-flight requests completed"))))
        fut))))

(defn- graceful-configurator
  "A configurator that waits for in-flight requests to complete before stopping
  Jetty on SIGTERM. Inserting a GracefulHandler at the head of the handler chain
  makes the server respond with 503 to new requests once shutdown begins, while
  waiting for in-flight requests to complete up to stop-timeout-ms."
  [stop-timeout-ms]
  (fn [^Server server]
    (.setStopTimeout server (long stop-timeout-ms))
    (.addEventListener server
                       (reify LifeCycle$Listener
                         (lifeCycleStopped [_ _]
                           (log/info "jetty server stopped"))))
    (let [^GracefulHandler graceful (logging-graceful-handler)]
      (.setHandler graceful (.getHandler server))
      (.setHandler server graceful))))

(def default-stop-timeout-ms
  "Default graceful shutdown stop timeout in milliseconds, used when
  :stop-timeout-ms is not specified. Adjust per product as needed via the
  Jetty9Server :graceful-shutdown {:stop-timeout-ms N} option, e.g. to fit the
  deployment platform's shutdown grace period."
  25000)

(defn- build-opts
  "Builds the opts passed to run-jetty. Graceful shutdown is enabled by default,
  installing a GracefulHandler with default-stop-timeout-ms; pass
  :graceful-shutdown {:stop-timeout-ms N} to override the timeout. If an explicit
  :configurator is given, it takes priority and graceful shutdown is not applied."
  [{:keys [graceful-shutdown configurator] :as opts}]
  (if configurator
    ;; When :configurator is given it takes priority, so graceful-shutdown is ignored.
    (dissoc opts :graceful-shutdown)
    (let [stop-timeout-ms (get graceful-shutdown :stop-timeout-ms default-stop-timeout-ms)]
      (when-not (and (integer? stop-timeout-ms) (pos? stop-timeout-ms))
        (throw (ex-info "Jetty9Server :graceful-shutdown requires a positive integer :stop-timeout-ms"
                        {:graceful-shutdown graceful-shutdown})))
      (-> opts
          (dissoc :graceful-shutdown)
          (assoc :configurator (graceful-configurator stop-timeout-ms))))))

(defrecord Jetty9Server [handler opts ^Server server]
  component/Lifecycle
  (start [this]
    (if server
      this
      (assoc this :server (jetty9/run-jetty (:handler handler) (build-opts opts)))))
  (stop [this]
    (when server
      (jetty9/stop-server server))
    (assoc this :server nil)))
