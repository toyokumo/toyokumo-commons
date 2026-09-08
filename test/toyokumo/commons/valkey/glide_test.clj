(ns toyokumo.commons.valkey.glide-test
  (:require
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [toyokumo.commons.health :as health]
   [toyokumo.commons.valkey.glide :as glide]
   [toyokumo.commons.valkey.glide.codec :as codec])
  (:import
   (glide.api
    BaseClient)
   (glide.api.models.exceptions
    ClosingException
    RequestException)))

;; Integration tests against the local Valkey started by `make docker-up` (localhost:6379, standalone).

(def test-conf
  {:host "localhost"
   :port 6379
   :request-timeout-ms 5000})

(def ^:private test-key-prefix "tc-glide-test:")

(defn- tk [suffix] (str test-key-prefix suffix))

(def ^:dynamic ^BaseClient *client* nil)

(defn- cleanup! [client]
  (when-let [ks (seq (glide/scan client (str test-key-prefix "*")))]
    (apply glide/del client ks)))

(use-fixtures
  :once
  (fn [f]
    (let [c (glide/create-client test-conf)]
      (binding [*client* c]
        (try
          (cleanup! c)
          (f)
          (finally
            (cleanup! c)
            (.close c)))))))

(deftest create-client-and-ping-test
  (testing "create-client returns a usable standalone client"
    (is (instance? BaseClient *client*))
    (is (= "PONG" (glide/ping *client*)))))

(deftest ->base-client-test
  (testing "accepts a raw BaseClient"
    (is (identical? *client* (glide/->base-client *client*))))
  (testing "accepts a map holding :client"
    (is (identical? *client* (glide/->base-client {:client *client*}))))
  (testing "rejects anything else"
    (is (thrown? IllegalArgumentException (glide/->base-client "nope")))))

(deftest encode-decode-respect-client-codec-test
  (testing "raw BaseClient falls back to the default codec"
    (is (= :kw (glide/decode *client* (glide/encode *client* :kw)))))
  (testing "a map client can carry its own codec"
    (let [c {:client *client* :codec codec/carmine-compat}]
      (is (= "kw" (glide/decode c (glide/encode c :kw)))))))

(deftest get-set-roundtrip-test
  (testing "default codec: full fidelity except integers"
    (glide/set *client* (tk "str") "hello")
    (is (= "hello" (glide/get *client* (tk "str"))))
    (glide/set *client* (tk "long") 42)
    (is (= "42" (glide/get *client* (tk "long")))
        "integers are stored as ASCII digits and decode as strings")
    (glide/set *client* (tk "kw") :ja)
    (is (= :ja (glide/get *client* (tk "kw")))
        "keywords round-trip with the default codec")
    (glide/set *client* (tk "map") {:a 1 :b "x" :c [1 2 3]})
    (is (= {:a 1 :b "x" :c [1 2 3]} (glide/get *client* (tk "map"))))
    (glide/set *client* (tk "nil") nil)
    (is (nil? (glide/get *client* (tk "nil"))))
    (is (true? (glide/exists? *client* (tk "nil")))
        "SET nil stores a marker; the key exists"))
  (testing "missing key"
    (is (nil? (glide/get *client* (tk "missing"))))))

(deftest getdel-test
  (testing "returns the value and deletes the key"
    (glide/set *client* (tk "getdel") {:a 1})
    (is (= {:a 1} (glide/getdel *client* (tk "getdel"))))
    (is (false? (glide/exists? *client* (tk "getdel"))))
    (is (nil? (glide/getdel *client* (tk "getdel")))
        "the key is gone, so a second call returns nil"))
  (testing "missing key"
    (is (nil? (glide/getdel *client* (tk "getdel-missing"))))))

(deftest set-options-test
  (testing ":ex sets a TTL"
    (glide/set *client* (tk "set-ex") "v" {:ex 60})
    (is (pos? (glide/ttl *client* (tk "set-ex")))))
  (testing ":nx only sets when the key does not exist"
    (glide/del *client* (tk "set-nx"))
    (is (= "OK" (glide/set *client* (tk "set-nx") "first" {:nx true})))
    (is (nil? (glide/set *client* (tk "set-nx") "second" {:nx true}))
        "returns nil when the condition is not met")
    (is (= "first" (glide/get *client* (tk "set-nx")))))
  (testing ":nx with :ex — atomic lock-acquisition idiom"
    (glide/del *client* (tk "lock"))
    (is (= "OK" (glide/set *client* (tk "lock") "owner" {:nx true :ex 60})))
    (is (nil? (glide/set *client* (tk "lock") "other" {:nx true :ex 60}))))
  (testing ":xx only sets when the key exists"
    (glide/del *client* (tk "set-xx"))
    (is (nil? (glide/set *client* (tk "set-xx") "v" {:xx true})))
    (glide/set *client* (tk "set-xx") "v1")
    (is (= "OK" (glide/set *client* (tk "set-xx") "v2" {:xx true})))))

(deftest setex-test
  (glide/setex *client* (tk "setex") 60 "value")
  (is (= "value" (glide/get *client* (tk "setex"))))
  (is (pos? (glide/ttl *client* (tk "setex")))))

(deftest del-test
  (glide/set *client* (tk "del:1") "a")
  (glide/set *client* (tk "del:2") "b")
  (is (= 2 (glide/del *client* (tk "del:1") (tk "del:2"))))
  (is (= 0 (glide/del *client* (tk "del:1"))))
  (is (= 0 (glide/del *client*))
      "no keys given: returns 0 without contacting the server"))

(deftest exists?-test
  (glide/set *client* (tk "exists") "v")
  (is (true? (glide/exists? *client* (tk "exists"))))
  (is (false? (glide/exists? *client* (tk "exists-missing")))))

(deftest counter-test
  (glide/del *client* (tk "counter"))
  (is (= 1 (glide/incr *client* (tk "counter"))))
  (is (= 11 (glide/incrby *client* (tk "counter") 10)))
  (is (= 10 (glide/decr *client* (tk "counter"))))
  (is (= 5 (glide/decrby *client* (tk "counter") 5)))
  (is (= "5" (glide/get *client* (tk "counter")))))

(deftest expire-ttl-test
  (glide/set *client* (tk "expire") "v")
  (is (true? (glide/expire *client* (tk "expire") 60))
      "expire returns a plain boolean (not carmine's 1/0)")
  (is (<= 1 (glide/ttl *client* (tk "expire")) 60))
  (is (false? (glide/expire *client* (tk "expire-missing") 60))))

(deftest mget-test
  (glide/set *client* (tk "mg:1") "a")
  (glide/set *client* (tk "mg:2") {:nested true})
  (glide/del *client* (tk "mg:missing"))
  (is (= ["a" {:nested true} nil]
         (glide/mget *client* [(tk "mg:1") (tk "mg:2") (tk "mg:missing")])))
  (is (= [] (glide/mget *client* []))
      "no keys given: returns [] without contacting the server"))

(deftest keys-and-scan-test
  (glide/set *client* (tk "ks:1") "a")
  (glide/set *client* (tk "ks:2") "b")
  (is (= [(tk "ks:1") (tk "ks:2")]
         (sort (glide/keys *client* (tk "ks:*")))))
  (is (= [(tk "ks:1") (tk "ks:2")]
         (sort (glide/scan *client* (tk "ks:*"))))))

(deftest set-commands-test
  (testing "sadd returns the number of members actually added"
    (is (= 3 (glide/sadd *client* (tk "set") "a" "b" "c")))
    (is (= 1 (glide/sadd *client* (tk "set") "b" "d"))
        "members already in the set are not counted")
    (is (thrown? clojure.lang.ArityException
          (apply glide/sadd *client* (tk "set") []))
        "at least one member is required"))
  (testing "srem returns the number of members actually removed"
    (is (= 2 (glide/srem *client* (tk "set") "a" "d")))
    (is (= 0 (glide/srem *client* (tk "set") "a"))
        "members not in the set are not counted")
    (is (thrown? clojure.lang.ArityException
          (apply glide/srem *client* (tk "set") []))
        "at least one member is required"))
  (testing "sscan returns one page and the cursor to resume from"
    (let [[next-cursor members] (glide/sscan *client* (tk "set") "0")]
      (is (= "0" next-cursor)
          "a small set is exhausted in one page")
      (is (= ["b" "c"] (sort members)))))
  (testing "sscan takes the cursor as a number or a string"
    (let [[next-cursor members] (glide/sscan *client* (tk "set") 0)]
      (is (= "0" next-cursor))
      (is (= ["b" "c"] (sort members)))))
  (testing "sscan paging covers every member"
    (let [expected (set (mapv #(str "m" %) (range 3000)))]
      (apply glide/sadd *client* (tk "set-big") expected)
      (loop [cursor "0"
             seen #{}
             pages 0]
        (let [[next-cursor members] (glide/sscan *client* (tk "set-big") cursor {:count 500})
              seen' (into seen members)
              pages' (inc pages)]
          (if (= "0" next-cursor)
            (do (is (= expected seen')
                    "paging to the end sees every member")
                (is (< 1 pages')
                    "a 3000-member set does not fit in one 500-member page"))
            (recur next-cursor seen' pages'))))))
  (testing "sscan match filters members the codec writes as plain text"
    (let [[_ members] (glide/sscan *client* (tk "set") "0" {:match "b"})]
      (is (= ["b"] members)))
    (glide/sadd *client* (tk "set-match") 42 "43")
    (is (= #{"42" "43"} (set (second (glide/sscan *client* (tk "set-match") "0" {:match "4*"}))))
        "integers are plain ASCII digits, so a pattern reaches them too"))
  (testing "sscan applies match and count together"
    (let [expected (set (filter #(re-matches #"m1\d*" %) (mapv #(str "m" %) (range 3000))))]
      (is (= 1111 (count expected)))
      (loop [cursor "0"
             seen #{}]
        (let [[next-cursor members] (glide/sscan *client* (tk "set-big") cursor
                                                 {:match "m1*" :count 500})
              seen' (into seen members)]
          (if (= "0" next-cursor)
            (is (= expected seen')
                "match filters server-side while count pages through the set")
            (recur next-cursor seen'))))))
  (testing "sscan rejects a :count that is not a positive integer"
    (doseq [cnt [0 -1 0.5 1.5 1/2]]
      (is (thrown? IllegalArgumentException
            (glide/sscan *client* (tk "set") "0" {:count cnt}))
          (str ":count " cnt " would be truncated to a bad COUNT"))))
  (testing "sscan rejects a nil cursor"
    (is (thrown? IllegalArgumentException
          (glide/sscan *client* (tk "set") nil))))
  (testing "sscan on a missing key is an immediately complete scan"
    (is (= ["0" []]
           (glide/sscan *client* (tk "set-missing") "0"))))
  (testing "members go through the client's codec"
    (glide/sadd *client* (tk "set-codec") :ja {:a 1} 42)
    (is (= #{:ja {:a 1} "42"}
           (set (second (glide/sscan *client* (tk "set-codec") "0"))))
        "integers are stored as ASCII digits and decode as strings")
    (is (= 1 (glide/srem *client* (tk "set-codec") {:a 1}))
        "re-encoding the same value byte for byte matches the stored member"))
  (testing "membership follows the encoded bytes, not Clojure equality"
    (glide/sadd *client* (tk "set-bytes") {:a 1 :b 2})
    (let [reordered (into {} [[:b 2] [:a 1]])]
      (is (= {:a 1 :b 2} reordered)
          "the two maps are equal as Clojure values")
      (is (= 0 (glide/srem *client* (tk "set-bytes") reordered))
          "but they encode differently, so srem does not reach the stored member")
      (is (= 1 (glide/sadd *client* (tk "set-bytes") reordered))
          "and adding it stores a second member")))
  (testing "set commands accept a map client"
    (let [c {:client *client* :codec codec/carmine-compat}]
      (is (= 1 (glide/sadd c (tk "set-map") :ja)))
      (is (= ["ja"] (second (glide/sscan c (tk "set-map") "0")))))))

(deftest info-test
  (let [s (glide/info *client*)]
    (is (string? s))
    (is (re-find #"redis_version|valkey_version" s)))
  (let [s (glide/info *client* :server)]
    (is (string? s))
    (is (re-find #"(?i)# server" s))))

(deftest exception-unwrap-test
  (testing "server errors surface as GLIDE exceptions, not ExecutionException"
    (glide/set *client* (tk "not-a-number") {:a 1})
    (is (thrown? RequestException
          (glide/incr *client* (tk "not-a-number")))))
  (testing "create-client against an unreachable server throws ClosingException"
    (is (thrown? ClosingException
          (glide/create-client {:host "localhost"
                                :port 3333
                                :request-timeout-ms 1000}))))
  (testing "commands on a closed client throw ClosingException"
    (let [c (glide/create-client test-conf)]
      (.close c)
      (is (thrown? ClosingException (glide/ping c))))))

(deftest glide-component-test
  (testing "Lifecycle"
    (let [c (component/start (glide/map->Glide {:config test-conf}))]
      (try
        (is (instance? BaseClient (:client c)))
        (is (identical? c (component/start c))
            "start is idempotent")
        (is (= "PONG" (glide/ping c))
            "commands accept the record directly")
        (finally
          (let [stopped (component/stop c)]
            (is (nil? (:client stopped)))
            (is (= test-conf (:config stopped))
                "config survives stop, so the component can be restarted"))))))
  (testing "codec is honoured by commands"
    (let [c (component/start (glide/map->Glide {:config test-conf
                                                :codec codec/carmine-compat}))]
      (try
        (glide/set c (tk "component-kw") :ja)
        (is (= "ja" (glide/get c (tk "component-kw"))))
        (finally
          (component/stop c)))))
  (testing "HealthCheck"
    (let [alive (component/start (glide/map->Glide {:config test-conf}))]
      (try
        (is (true? (health/alive? alive)))
        (finally
          (component/stop alive))))
    (let [dead (glide/map->Glide {:config {:host "localhost"
                                           :port 3333
                                           :request-timeout-ms 500}})]
      ;; not started: no client, ping throws, -alive? catches and returns false
      (is (false? (health/alive? dead))))))
