(ns toyokumo.commons.graceful-shutdown
  (:require
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(defmacro with-graceful-shutdown
  "Executes the body with a graceful shutdown mechanism.
  When a shutdown signal (SIGTERM) is received, it waits for the specified timeout
  before proceeding with the shutdown.
  Notes:
  - If the body is asynchronous, it does not wait for its completion before proceeding with the shutdown."
  [timeout-ms & body]
  `(let [end-chan# (async/chan 1)
         shutdown-handler# (Thread.
                            (fn []
                              (log/infof "graceful-shutdown waiting")
                              (let [timeout-chan# (async/timeout ~timeout-ms)
                                    [_# p#] (async/alts!! [end-chan# timeout-chan#])]
                                (if (= p# timeout-chan#)
                                  (log/infof "graceful-shutdown timeout")
                                  (log/infof "graceful-shutdown end")))))]
     (try
       (.addShutdownHook (Runtime/getRuntime)
                         shutdown-handler#)
       ~@body
       (finally
         (async/put! end-chan# :done (fn [_#] (async/close! end-chan#)))
         (try
           (.removeShutdownHook (Runtime/getRuntime)
                                shutdown-handler#)
           (catch IllegalStateException _#
             ;; the shutdown sequence has already begun
             nil))))))
