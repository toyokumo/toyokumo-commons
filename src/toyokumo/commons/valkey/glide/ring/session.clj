(ns toyokumo.commons.valkey.glide.ring.session
  "Ring SessionStore backed by the Valkey GLIDE client; the counterpart of carmine's
  `taoensso.carmine.ring/carmine-store`.

  Session values go through the codec attached to the client, so a product migrating from carmine can read its existing
  sessions by attaching the `carmine-compat` codec to the component (see `toyokumo.commons.valkey.glide.codec`).

  This namespace is also a reference implementation of building on the public building blocks: it only uses
  `glide/get`, `glide/setex` and `glide/del`."
  (:require
   [ring.middleware.session.store :as ss]
   [toyokumo.commons.valkey.glide :as glide]))

(def ^:private default-expiration-secs
  (* 60 60 24 30)) ; 30 days

(defrecord GlideSessionStore [client key-prefix expiration-secs]
  ss/SessionStore
  (read-session [_ k]
    (when k
      (glide/get client k)))
  (write-session [_ k data]
    (let [k (or k (str key-prefix ":" (random-uuid)))]
      (glide/setex client k expiration-secs data)
      k))
  (delete-session [_ k]
    (when k
      (glide/del client k))
    nil))

(defn glide-store
  "Returns a Ring SessionStore backed by `client` (a `BaseClient` or a map with `:client`, such as the `Glide`
  component record).

  options:
    :key-prefix       required; prefix of generated session keys
    :expiration-secs  TTL for written sessions; default 30 days

  Passing an explicit nil :expiration-secs stores sessions without a TTL (persist until deleted)."
  [client {:keys [key-prefix expiration-secs]
           :or {expiration-secs default-expiration-secs}}]
  (when-not (string? key-prefix)
    (throw (ex-info ":key-prefix must be a string" {:key-prefix key-prefix})))
  (->GlideSessionStore client key-prefix expiration-secs))
