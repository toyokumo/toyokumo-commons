(ns toyokumo.commons.valkey.glide.carmine-compat-test
  "Cross-compatibility tests between carmine and the glide client with the carmine-compat codec, through a real server.
  These guarantee the mixed carmine/glide deployment window during a migration."
  (:require
   [clojure.test :refer :all]
   [taoensso.carmine :as car]
   [toyokumo.commons.valkey.glide :as glide]
   [toyokumo.commons.valkey.glide.codec :as codec])
  (:import
   (glide.api
    BaseClient)))

(def ^:private test-key-prefix "tc-glide-ccr:")

(defn- tk [suffix] (str test-key-prefix suffix))

(def carmine-conf
  {:pool {}
   :spec {:host "localhost" :port 6379}})

(def ^:dynamic *client* nil)

(defn- cleanup! [client]
  (when-let [ks (seq (glide/scan client (str test-key-prefix "*")))]
    (apply glide/del client ks)))

(use-fixtures
  :once
  (fn [f]
    (let [c (glide/create-client {:host "localhost"
                                  :port 6379
                                  :request-timeout-ms 5000})
          wrapped {:client c :codec codec/carmine-compat}]
      (binding [*client* wrapped]
        (try
          (cleanup! wrapped)
          (f)
          (finally
            (cleanup! wrapped)
            (.close ^BaseClient c)))))))

(deftest carmine-write-glide-read-test
  (testing "values written by carmine decode identically via glide"
    (car/wcar carmine-conf (car/set (tk "long") 42))
    (is (= "42" (glide/get *client* (tk "long"))))
    (car/wcar carmine-conf (car/set (tk "str") "hello"))
    (is (= "hello" (glide/get *client* (tk "str"))))
    (car/wcar carmine-conf (car/set (tk "kw") :ja))
    (is (= "ja" (glide/get *client* (tk "kw"))))
    (car/wcar carmine-conf (car/set (tk "map") {:a 1 :b [1 2]}))
    (is (= {:a 1 :b [1 2]} (glide/get *client* (tk "map"))))
    (car/wcar carmine-conf (car/set (tk "nil") nil))
    (is (nil? (glide/get *client* (tk "nil"))))
    (car/wcar carmine-conf (car/incr (tk "counter")))
    (is (= 2 (glide/incr *client* (tk "counter")))))
  (testing "byte arrays written by carmine decode as byte arrays via glide"
    (car/wcar carmine-conf (car/set (tk "bytes") (byte-array [1 2 3 4 5])))
    (let [v (glide/get *client* (tk "bytes"))]
      (is (bytes? v))
      (is (= [1 2 3 4 5] (vec v))))))

(deftest glide-write-carmine-read-test
  (testing "values written by glide (carmine-compat codec) decode identically via carmine"
    (glide/set *client* (tk "g:long") 42)
    (is (= "42" (car/wcar carmine-conf (car/get (tk "g:long")))))
    (glide/set *client* (tk "g:str") "hello")
    (is (= "hello" (car/wcar carmine-conf (car/get (tk "g:str")))))
    (glide/set *client* (tk "g:kw") :ja)
    (is (= "ja" (car/wcar carmine-conf (car/get (tk "g:kw")))))
    (glide/set *client* (tk "g:map") {:a 1 :b [1 2]})
    (is (= {:a 1 :b [1 2]} (car/wcar carmine-conf (car/get (tk "g:map")))))
    (glide/set *client* (tk "g:nil") nil)
    (is (nil? (car/wcar carmine-conf (car/get (tk "g:nil")))))
    (glide/incr *client* (tk "g:counter"))
    (is (= 2 (car/wcar carmine-conf (car/incr (tk "g:counter"))))))
  (testing "byte arrays written by glide (carmine-compat codec) decode as byte arrays via carmine"
    (glide/set *client* (tk "g:bytes") (byte-array [5 4 3 2 1]))
    (let [v (car/wcar carmine-conf (car/get (tk "g:bytes")))]
      (is (bytes? v))
      (is (= [5 4 3 2 1] (vec v))))))

(deftest default-codec-reads-carmine-written-data-test
  (testing "the default codec decodes carmine-written data identically (supported codec-switch path)"
    (let [default-client (assoc *client* :codec codec/default)]
      (car/wcar carmine-conf (car/set (tk "d:str") "hello"))
      (is (= "hello" (glide/get default-client (tk "d:str"))))
      (car/wcar carmine-conf (car/set (tk "d:kw") :ja))
      (is (= "ja" (glide/get default-client (tk "d:kw")))
          "carmine-written keywords still decode as strings under default")
      (car/wcar carmine-conf (car/set (tk "d:map") {:a 1}))
      (is (= {:a 1} (glide/get default-client (tk "d:map"))))
      (car/wcar carmine-conf (car/set (tk "d:nil") nil))
      (is (nil? (glide/get default-client (tk "d:nil")))))))
