(ns toyokumo.commons.valkey.glide.codec-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.valkey.glide.codec :as codec])
  (:import
   (clojure.lang
    ExceptionInfo)
   (java.nio.charset
    StandardCharsets)))

(defn- roundtrip [codec v]
  ((:decode codec) ((:encode codec) v)))

(deftest default-codec-test
  (testing "strings and integers are stored plain; integers read back as strings"
    (is (= "hello" (roundtrip codec/default "hello")))
    (is (= "42" (roundtrip codec/default 42))
        "integers are ASCII on the wire (so INCR works); they decode as strings")
    (is (= "42" (String. ^bytes ((:encode codec/default) 42) StandardCharsets/UTF_8))))

  (testing "everything else round-trips with full type fidelity"
    (is (= :ja (roundtrip codec/default :ja)))
    (is (= :ns/kw (roundtrip codec/default :ns/kw)))
    (is (= 1.5 (roundtrip codec/default 1.5)))
    (is (= {:a 1 :b "x" :c [1 2 3]} (roundtrip codec/default {:a 1 :b "x" :c [1 2 3]})))
    (is (= #{1 2} (roundtrip codec/default #{1 2})))
    (let [ba (byte-array [1 2 3])
          out (roundtrip codec/default ba)]
      (is (bytes? out))
      (is (= [1 2 3] (vec out)))))

  (testing "nil round-trips via the nil marker"
    (is (nil? (roundtrip codec/default nil)))
    (is (= [0 0x5F] (mapv #(bit-and % 0xff) ((:encode codec/default) nil)))))

  (testing "decode of nil input is nil"
    (is (nil? ((:decode codec/default) nil))))

  (testing "strings beginning with the reserved null byte are rejected"
    (is (thrown? ExceptionInfo
          (roundtrip codec/default (str (char 0) "x"))))))

(deftest carmine-compat-codec-test
  (testing "carmine's lossy scalar behaviour is reproduced"
    (is (= "ja" (roundtrip codec/carmine-compat :ja))
        "keywords are written as their name, so they decode as strings")
    (is (= "ns/kw" (roundtrip codec/carmine-compat :ns/kw)))
    (is (= "1.5" (roundtrip codec/carmine-compat 1.5))
        "doubles are written as ASCII, so they decode as strings"))

  (testing "byte arrays are written with carmine's `\\0<` binary marker and round-trip as byte arrays"
    (let [ba (.getBytes "raw" StandardCharsets/UTF_8)
          wire ((:encode codec/carmine-compat) ba)]
      (is (= [0x00 0x3C] (mapv #(bit-and % 0xff) (take 2 wire)))
          "wire starts with carmine's \\0< binary marker")
      (is (= [114 97 119] (vec (drop 2 wire))))
      (let [out (roundtrip codec/carmine-compat ba)]
        (is (bytes? out))
        (is (= [114 97 119] (vec out))
            "byte arrays round-trip losslessly, matching carmine (not lossy here)"))))

  (testing "shared behaviour with default"
    (is (= "hello" (roundtrip codec/carmine-compat "hello")))
    (is (= "42" (roundtrip codec/carmine-compat 42)))
    (is (nil? (roundtrip codec/carmine-compat nil)))
    (is (= {:a 1} (roundtrip codec/carmine-compat {:a 1})))))

(defn- comparable
  "byte[] doesn't implement value equality, so compare as a vector."
  [x]
  (if (bytes? x) (vec x) x))

(deftest default-reads-carmine-wire-test
  (testing "everything carmine-compat writes decodes identically with default (guaranteed migration path)"
    (doseq [v ["hello" 42 :ja 1.5 nil {:a 1 :b [1 2]} #{:x} (byte-array [9 8 7])]]
      (is (= (comparable ((:decode codec/carmine-compat) ((:encode codec/carmine-compat) v)))
             (comparable ((:decode codec/default) ((:encode codec/carmine-compat) v))))
          (str "value: " (pr-str v))))))
