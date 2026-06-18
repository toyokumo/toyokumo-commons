(ns toyokumo.commons.server.jetty9-test
  (:require
   [clj-http.client :as client]
   [clojure.test :refer :all]
   [clojure.tools.logging.test :as tlt]
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty9 :as jetty9]
   [toyokumo.commons.ring.response :as res]
   [toyokumo.commons.server.jetty9 :as sut :refer [map->Jetty9Server]])
  (:import
   (java.util.concurrent
    CompletableFuture
    TimeUnit)
   (org.eclipse.jetty.server
    Server)
   (org.eclipse.jetty.server.handler
    DefaultHandler
    GracefulHandler)))

(defrecord TestHandler [handler])

(deftest map->Jetty9Server-test
  (testing "Start and stop server"
    (let [server (-> {:handler (map->TestHandler
                                {:handler (fn [_]
                                            (-> (res/ok "Hello world!")
                                                (res/html)))})
                      :opts {:host "127.0.0.1"
                             :port 9999
                             :join? false}}
                     (map->Jetty9Server)
                     (component/start))]
      (try
        (is (not (nil? (:server server))))
        (is (= {:status 200
                :body "Hello world!"
                :headers {"Content-Type" "text/html"}}
               (-> (client/get "http://127.0.0.1:9999/hello")
                   (select-keys [:status :body :headers])
                   (update :headers #(select-keys % ["Content-Type"])))))
        (finally
          (let [server (component/stop server)]
            (is (nil? (:server server)))))))))

(deftest logging-graceful-handler-test
  (testing "calling shutdown emits the start-wait log and returns a future"
    (tlt/with-log
      (let [handler (#'sut/logging-graceful-handler)
            fut (.shutdown ^GracefulHandler handler)]
        (is (instance? CompletableFuture fut))
        (is (tlt/logged? 'toyokumo.commons.server.jetty9 :info
                         #"graceful shutdown start\. in-flight requests:\d+")))))
  (testing "with no in-flight requests the future completes immediately and the end-wait log is emitted"
    (tlt/with-log
      (let [handler (#'sut/logging-graceful-handler)
            fut (.shutdown ^GracefulHandler handler)]
        (.get ^CompletableFuture fut 1 TimeUnit/SECONDS)
        (is (tlt/logged? 'toyokumo.commons.server.jetty9 :info
                         #"graceful shutdown end\. all in-flight requests completed"))))))

(deftest graceful-configurator-test
  (testing "sets the stop timeout and inserts a GracefulHandler at the head of the handler chain"
    (let [server (Server.)
          original-handler (DefaultHandler.)
          stop-timeout-ms 12345]
      (.setHandler server original-handler)
      ((#'sut/graceful-configurator stop-timeout-ms) server)
      (is (= stop-timeout-ms (.getStopTimeout server)))
      (let [handler (.getHandler server)]
        (is (instance? GracefulHandler handler))
        (is (identical? original-handler
                        (.getHandler ^GracefulHandler handler))))))
  (testing "emits the server-stopped log after the server stops"
    (tlt/with-log
      (let [server (Server.)]
        (.setHandler server (DefaultHandler.))
        ((#'sut/graceful-configurator 1000) server)
        (.start server)
        (.stop server))
      (is (tlt/logged? 'toyokumo.commons.server.jetty9 :info
                       #"jetty server stopped"))))
  (testing "stop blocks until the in-flight request drains"
    (let [started (promise)
          release (promise)
          server (jetty9/run-jetty
                  (fn [_]
                    (deliver started true)
                    @release
                    (res/ok "done"))
                  {:host "127.0.0.1"
                   :port 9998
                   :join? false
                   :configurator (#'sut/graceful-configurator 10000)})
          resp (future (client/get "http://127.0.0.1:9998/slow"
                                   {:throw-exceptions false
                                    :connection-timeout 5000
                                    :socket-timeout 10000}))]
      (try
        ;; Wait until the request is actually being processed (in-flight).
        (is (true? (deref started 5000 ::timeout))
            "the request must start being processed")
        ;; Start stop on another thread. While an in-flight request remains, it blocks to drain.
        (let [stop-fut (future (jetty9/stop-server server))]
          ;; Without a GracefulHandler stop would complete immediately, so it being
          ;; incomplete here proves draining.
          (Thread/sleep 500)
          (is (not (realized? stop-fut))
              "stop must not complete while an in-flight request remains")
          ;; Release the in-flight request so it can complete.
          (deliver release true)
          ;; The in-flight request completes with 200 without being interrupted.
          (let [response (deref resp 10000 ::timeout)]
            (is (not= ::timeout response) "the request must complete")
            (is (= 200 (:status response))))
          ;; Draining done, so stop must complete.
          (is (not= ::timeout (deref stop-fut 10000 ::timeout))
              "stop must complete after the in-flight request drains"))
        (finally
          ;; Always release the request and stop the server, even on assertion failure.
          (deliver release true)
          (jetty9/stop-server server))))))
