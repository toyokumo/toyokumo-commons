(ns toyokumo.commons.valkey.glide.ring.session-test
  (:require
   [clojure.test :refer :all]
   [ring.middleware.session.store :as ss]
   [toyokumo.commons.valkey.glide :as glide]
   [toyokumo.commons.valkey.glide.ring.session :as session])
  (:import
   (clojure.lang
    ExceptionInfo)
   (glide.api
    BaseClient)))

(def ^:private test-key-prefix "tc-glide-session-test")

(def ^:dynamic *client* nil)

(defn- cleanup! [client]
  (when-let [ks (seq (glide/scan client (str test-key-prefix "*")))]
    (apply glide/del client ks)))

(use-fixtures
  :once
  (fn [f]
    (let [c (glide/create-client {:host "localhost"
                                  :port 6379
                                  :request-timeout-ms 5000})]
      (binding [*client* c]
        (try
          (cleanup! c)
          (f)
          (finally
            (cleanup! c)
            (.close ^BaseClient c)))))))

(deftest glide-store-test
  (let [store (session/glide-store *client* {:key-prefix test-key-prefix})]
    (testing "write generates a prefixed key and read returns the data"
      (let [data {:user-id 1 :roles [:admin]}
            k (ss/write-session store nil data)]
        (is (string? k))
        (is (.startsWith ^String k (str test-key-prefix ":")))
        (is (= data (ss/read-session store k)))
        (testing "write with an existing key overwrites and keeps the key"
          (is (= k (ss/write-session store k {:user-id 2})))
          (is (= {:user-id 2} (ss/read-session store k))))
        (testing "the session key has a TTL"
          (is (pos? (glide/ttl *client* k))))
        (testing "delete removes the session and returns nil"
          (is (nil? (ss/delete-session store k)))
          (is (nil? (ss/read-session store k))))))
    (testing "read of nil / missing key returns nil"
      (is (nil? (ss/read-session store nil)))
      (is (nil? (ss/read-session store (str test-key-prefix ":missing")))))))

(deftest glide-store-validation-test
  (testing ":key-prefix is required"
    (is (thrown? ExceptionInfo
          (session/glide-store *client* {})))
    (is (thrown? ExceptionInfo
          (session/glide-store *client* {:key-prefix :not-a-string})))))
