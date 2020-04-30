(ns toyokumo.commons.transit-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.transit :refer [transit-encode transit-decode]])
  (:import
   (java.util
    UUID)))

(deftest transit-encode-test
  (let [uuid (UUID/randomUUID)]
    (is (= (format "[\"^ \",\"~:id\",1,\"~:uuid\",\"~u%s\",\"~:kw\",\"^2\",\"~:name\",\"foo\",\"~:some-key\",3.3]"
                   uuid)
           (transit-encode {:id 1
                            :uuid uuid
                            :kw :kw
                            :name "foo"
                            :some-key 3.3})))))

(deftest transit-decode-test
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
