(ns toyokumo.commons.server.jetty9-test
  (:require
   [clj-http.client :as client]
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [toyokumo.commons.ring.response :as res]
   [toyokumo.commons.server.jetty9 :refer [map->Jetty9Server]]))

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
