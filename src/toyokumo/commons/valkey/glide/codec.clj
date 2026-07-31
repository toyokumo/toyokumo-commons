(ns toyokumo.commons.valkey.glide.codec
  "Value <-> byte[] codecs for the Valkey GLIDE client.

  A codec is a plain map of `{:encode (fn [v] bytes), :decode (fn [bytes] v)}`, so applications can supply their own.

  Two codecs are provided:

  - `default` — for new development. Contract: everything round-trips with full type fidelity, except integers, which
    are stored as plain ASCII digits (so server-side INCR/DECR work) and therefore decode as strings.
  - `carmine-compat` — writes byte-identical to carmine's smart serialization, for migrating from carmine without a
    data migration. Reproduces carmine's lossy behaviour: keywords and doubles are written as plain strings and decode
    as strings. Byte arrays use carmine's `\\0<` binary marker and round-trip as byte arrays (carmine is not lossy
    here). Use this while old (carmine) and new (glide) versions read and write the same data concurrently.

  Both codecs share the same decoder, and `default` can decode everything `carmine-compat` (or carmine itself) has
  written. Switching a product from `carmine-compat` to `default` is therefore a supported path; only the decoded type
  of keywords/doubles written *after* the switch changes."
  (:require
   [taoensso.nippy :as nippy])
  (:import
   (java.nio.charset
    StandardCharsets)
   (java.util
    Arrays)))

;; Wire markers, identical to carmine's smart serialization.
(def ^:private ^:const clj-marker-1 (unchecked-byte 0x00))
(def ^:private ^:const clj-marker-2 (unchecked-byte 0x3E)) ; '>'
(def ^:private ^:const nil-marker-2 (unchecked-byte 0x5F)) ; '_'
(def ^:private ^:const bin-marker-2 (unchecked-byte 0x3C)) ; '<'

(defn- clj-marked?
  [^bytes ba]
  (and (>= (alength ba) 2)
       (= (aget ba 0) clj-marker-1)
       (= (aget ba 1) clj-marker-2)))

(defn- nil-marked?
  [^bytes ba]
  (and (= (alength ba) 2)
       (= (aget ba 0) clj-marker-1)
       (= (aget ba 1) nil-marker-2)))

(defn- bin-marked?
  [^bytes ba]
  (and (>= (alength ba) 2)
       (= (aget ba 0) clj-marker-1)
       (= (aget ba 1) bin-marker-2)))

(defn- decode-bytes
  [^bytes ba]
  (cond
    (nil? ba) nil
    (clj-marked? ba) (nippy/thaw (Arrays/copyOfRange ba 2 (alength ba)))
    (nil-marked? ba) nil
    (bin-marked? ba) (Arrays/copyOfRange ba 2 (alength ba))
    :else (String. ba 0 (alength ba) StandardCharsets/UTF_8)))

(defn- nil-bytes
  ^bytes []
  (byte-array [clj-marker-1 nil-marker-2]))

(defn- integer-bytes
  ^bytes [v]
  (.getBytes (Long/toString (long v)) StandardCharsets/UTF_8))

(defn- string-bytes
  "Guards against colliding with the reserved marker byte (0x00): carmine's `ensure-reserved-first-byte` does the same."
  ^bytes [^String s]
  (let [ba (.getBytes s StandardCharsets/UTF_8)]
    (when (and (pos? (alength ba)) (= (aget ba 0) clj-marker-1))
      (throw (ex-info "value strings must not begin with the reserved null byte"
                      {:value s})))
    ba))

(defn- marked-bytes
  "Concatenates the 2-byte marker `[b0 b1]` with the raw payload `ba`."
  ^bytes [b0 b1 ^bytes ba]
  (let [out (byte-array (+ 2 (alength ba)))]
    (aset-byte out 0 b0)
    (aset-byte out 1 b1)
    (System/arraycopy ba 0 out 2 (alength ba))
    out))

(defn- nippy-bytes
  ^bytes [v]
  (marked-bytes clj-marker-1 clj-marker-2 (nippy/freeze v)))

(defn- bin-bytes
  ^bytes [^bytes ba]
  (marked-bytes clj-marker-1 bin-marker-2 ba))

(defn- integer-value?
  [v]
  (or (instance? Long v)
      (instance? Integer v)
      (instance? Short v)
      (instance? Byte v)))

(defn- encode-default
  ^bytes [v]
  (cond
    (nil? v) (nil-bytes)
    (integer-value? v) (integer-bytes v)
    (string? v) (string-bytes v)
    :else (nippy-bytes v)))

(defn- encode-carmine-compat
  ^bytes [v]
  (cond
    (nil? v) (nil-bytes)
    (integer-value? v) (integer-bytes v)
    (or (instance? Double v) (instance? Float v)) (string-bytes (str v))
    (string? v) (string-bytes v)
    (keyword? v) (string-bytes (if-let [n (namespace v)]
                                 (str n "/" (name v))
                                 (name v)))
    (bytes? v) (bin-bytes v)
    :else (nippy-bytes v)))

(def default
  "Codec for new development. See the namespace docstring for the contract."
  {:encode encode-default
   :decode decode-bytes})

(def carmine-compat
  "Codec that writes byte-identical to carmine's smart serialization. See the namespace docstring for when to use it."
  {:encode encode-carmine-compat
   :decode decode-bytes})
