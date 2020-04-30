(ns toyokumo.commons.json-test
  (:require
   [clojure.test :refer :all]
   [helper :as h]
   [toyokumo.commons.json :refer [json-encode json-decode]])
  (:import
   (java.util
    UUID)))

(use-fixtures :once h/enable-validation-fixture)

(deftest json-encode-test
  (let [uuid (UUID/randomUUID)]
    (is (= (format "{\"id\":1,\"uuid\":\"%s\",\"kw\":\"kw\",\"name\":\"foo\",\"someKey\":3.3}"
                   uuid)
           (json-encode {:id 1
                         :uuid uuid
                         :kw :kw
                         :name "foo"
                         :some-key 3.3})))))

(deftest json-decode-test
  (let [uuid (UUID/randomUUID)]
    (is (= {:id 1
            :uuid (str uuid)
            :kw "kw"
            :name "foo"
            :some-key 3.3}
           (json-decode (json-encode {:id 1
                                      :uuid uuid
                                      :kw :kw
                                      :name "foo"
                                      :some-key 3.3}))))))
