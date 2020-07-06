(ns toyokumo.commons.transit-test
  (:require
   [clojure.test :refer :all]
   [cognitect.transit :as t]
   [toyokumo.commons.transit :refer [transit-encode transit-decode]])
  (:import
   (java.time
    Instant)
   (java.util
    UUID)))

(def instant-write-handler
  {Instant (t/write-handler
            (constantly "Instant") #(.toString %))})

(def instant-read-handler
  {"Instant" (t/read-handler
              #(Instant/parse %))})

(deftest transit-encode-test
  (testing "default encode"
    (let [uuid (UUID/randomUUID)]
      (is (= (format "[\"^ \",\"~:id\",1,\"~:uuid\",\"~u%s\",\"~:kw\",\"^2\",\"~:name\",\"foo\",\"~:some-key\",3.3]"
                     uuid)
             (transit-encode {:id 1
                              :uuid uuid
                              :kw :kw
                              :name "foo"
                              :some-key 3.3})))))
  (testing "type extension"
    (is (string? (transit-encode {:now (Instant/now)} {:opts {:handlers instant-write-handler}})))))

(deftest transit-decode-test
  (testing "default decode"
    (let [uuid (UUID/randomUUID)]
      (is (= {:id 1
              :uuid uuid
              :kw :kw
              :name "foo"
              :some-key 3.3}
             (transit-decode (transit-encode {:id 1
                                              :uuid uuid
                                              :kw :kw
                                              :name "foo"
                                              :some-key 3.3}))))))
  (testing "type extension"
    (let [now (Instant/now)]
      (is (= {:now now}
             (transit-decode
              (transit-encode {:now now} {:opts {:handlers instant-write-handler}})
              {:opts {:handlers instant-read-handler}}))))))
