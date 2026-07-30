(ns toyokumo.commons.valkey.glide
  "Valkey client on top of the Valkey GLIDE Java SDK.

  - Synchronous API: GLIDE returns `CompletableFuture`s; this wrapper blocks on them and unwraps `ExecutionException`
    so callers catch GLIDE's own exception types.
  - Values go through the codec attached to the client (see `toyokumo.commons.valkey.glide.codec`); a raw `BaseClient`
    uses the default codec.
  - Commands accept either a `BaseClient` or a map with a `:client` key (such as the `Glide` component record below),
    optionally with `:codec`.
  - Commands not provided here can be defined in application code from the public low-level building blocks:
    `->base-client`, `str->gs`, `bytes->gs`, `gs->bytes`, `gs->str`, `encode`, `decode` and `fut-get`."
  (:refer-clojure :exclude [get keys set])
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [toyokumo.commons.health :as health]
   [toyokumo.commons.valkey.glide.codec :as codec])
  (:import
   (glide.api
    BaseClient
    GlideClient
    GlideClusterClient)
   (glide.api.models
    ClusterValue
    GlideString)
   (glide.api.models.commands
    InfoOptions$Section
    SetOptions
    SetOptions$ConditionalSet
    SetOptions$Expiry)
   (glide.api.models.commands.scan
    ClusterScanCursor
    ScanOptions)
   (glide.api.models.configuration
    GlideClientConfiguration
    GlideClusterClientConfiguration
    NodeAddress
    ReadFrom
    ServerCredentials)
   (glide.internal
    AsyncRegistry)
   (java.nio.charset
    StandardCharsets)
   (java.util.concurrent
    CompletableFuture
    ExecutionException)))

;;; ---- client lifecycle ----------------------------------------------------

;; GLIDE registers its own JVM shutdown hook in a static initializer. That hook closes the async runtime in parallel
;; with an application's graceful shutdown (e.g. Jetty9Server draining in-flight requests after SIGTERM), which makes
;; in-flight work fail with ClosingException. Removing the hook is safe: GLIDE's Java-side threads are daemon threads,
;; so JVM exit still proceeds normally and resources are reclaimed at process exit.
(defonce ^:private glide-shutdown-hook-removed
  (delay (AsyncRegistry/removeShutdownHook)))

(defn- ->server-credentials
  ^ServerCredentials [{:keys [password username]}]
  (when (or password username)
    (let [b (ServerCredentials/builder)]
      (when password (.password b password))
      (when username (.username b username))
      (.build b))))

(defn- ->read-from
  ^ReadFrom [k]
  (case k
    :primary ReadFrom/PRIMARY
    :prefer-replica ReadFrom/PREFER_REPLICA
    :az-affinity ReadFrom/AZ_AFFINITY
    :az-affinity-replicas-and-primary ReadFrom/AZ_AFFINITY_REPLICAS_AND_PRIMARY
    :all-nodes ReadFrom/ALL_NODES
    ReadFrom/PRIMARY))

(defn- node-address
  ^NodeAddress [host port]
  (-> (NodeAddress/builder)
      (.host host)
      (.port (int port))
      (.build)))

(defn fut-get
  "Blocks on a GLIDE `CompletableFuture` and unwraps `ExecutionException`, so callers see GLIDE's own exception types.
  Use this when defining commands not provided by this namespace."
  [^CompletableFuture fut]
  (try
    (.get fut)
    (catch ExecutionException e
      (throw (or (.getCause e) e)))))

(defn create-client
  "Opens a GLIDE client. Returns a `BaseClient` (`GlideClient` for standalone, `GlideClusterClient` when
  `:cluster-mode?` is true).

  config:
    {:host \"localhost\"     ; default \"localhost\"
     :port 6379              ; default 6379
     :password nil           ; optional
     :username nil           ; optional
     :use-tls? false         ; default false
     :request-timeout-ms nil ; optional
     :database-id nil        ; standalone only
     :read-from :primary     ; :primary / :prefer-replica / :az-affinity /
                             ; :az-affinity-replicas-and-primary / :all-nodes
     :client-az nil          ; availability zone for AZ affinity
     :cluster-mode? false    ; default false (= standalone)
     :remove-glide-shutdown-hook? true}

  `:read-from` selects replica reads per *client*; there is no per-command switch. Create separate clients if some
  reads must go to the primary.

  `:remove-glide-shutdown-hook?` (default true) removes GLIDE's own JVM shutdown hook once, on first client creation,
  so that it does not race with the application's graceful shutdown (see Jetty9Server, which drains in-flight requests
  on SIGTERM by default). Pass false to keep the hook."
  ^BaseClient [{:keys [host port password username use-tls? request-timeout-ms
                       database-id read-from client-az cluster-mode?
                       remove-glide-shutdown-hook?]
                :or {host "localhost"
                     port 6379
                     use-tls? false
                     read-from :primary
                     remove-glide-shutdown-hook? true}}]
  (when remove-glide-shutdown-hook?
    @glide-shutdown-hook-removed)
  (let [addr (node-address host port)
        creds (->server-credentials {:password password :username username})
        rf (->read-from read-from)]
    (if cluster-mode?
      (let [b (-> (GlideClusterClientConfiguration/builder)
                  (.address addr)
                  (.useTLS use-tls?)
                  (.readFrom rf))]
        (when creds (.credentials b creds))
        (when request-timeout-ms (.requestTimeout b (int request-timeout-ms)))
        (when client-az (.clientAZ b client-az))
        (fut-get (GlideClusterClient/createClient (.build b))))
      (let [b (-> (GlideClientConfiguration/builder)
                  (.address addr)
                  (.useTLS use-tls?)
                  (.readFrom rf))]
        (when creds (.credentials b creds))
        (when request-timeout-ms (.requestTimeout b (int request-timeout-ms)))
        (when database-id (.databaseId b (int database-id)))
        (fut-get (GlideClient/createClient (.build b)))))))

;;; ---- low-level building blocks --------------------------------------------

(defn ->base-client
  "Returns the underlying `BaseClient`. Accepts either a `BaseClient` itself or a map with a `:client` key (such as the
  `Glide` component record)."
  ^BaseClient [x]
  (cond
    (instance? BaseClient x) x
    (and (map? x) (instance? BaseClient (:client x))) (:client x)
    :else (throw (IllegalArgumentException.
                  "expected BaseClient or a map with :client BaseClient"))))

(defn- client-codec
  [client]
  (or (when (map? client) (:codec client))
      codec/default))

(defn encode
  "Encodes `v` to bytes using the codec attached to `client`."
  ^bytes [client v]
  ((:encode (client-codec client)) v))

(defn decode
  "Decodes bytes to a value using the codec attached to `client`."
  [client ba]
  ((:decode (client-codec client)) ba))

(defn str->gs
  ^GlideString [^String s]
  (GlideString/gs (.getBytes s StandardCharsets/UTF_8)))

(defn bytes->gs
  ^GlideString [^bytes ba]
  (GlideString/gs ba))

(defn gs->bytes
  ^bytes [^GlideString gs]
  (when gs (.getBytes gs)))

(defn gs->str
  ^String [^GlideString gs]
  (when gs (String. (.getBytes gs) StandardCharsets/UTF_8)))

(defn- gs-array
  "Builds a GlideString[] for GLIDE's multi-key APIs. Array classes cannot be imported, so the binary-name type hint
  lives here once."
  ^"[Lglide.api.models.GlideString;" [gss]
  (into-array GlideString gss))

;;; ---- commands --------------------------------------------------------------

(defn ping
  "Returns \"PONG\"."
  ^String [client]
  (let [c (->base-client client)]
    (if (instance? GlideClusterClient c)
      (fut-get (.ping ^GlideClusterClient c))
      (fut-get (.ping ^GlideClient c)))))

(defn get
  "Returns the value of key `k`, or nil if it does not exist."
  [client ^String k]
  (when-let [^GlideString gs (fut-get (.get (->base-client client) (str->gs k)))]
    (decode client (gs->bytes gs))))

(defn set
  "Sets key `k` to `v`. Returns \"OK\", or nil when `:nx`/`:xx` is given and the condition is not met.

  options:
    :ex      expire in seconds
    :px      expire in milliseconds
    :keepttl keep the existing TTL
    :nx      only set if the key does not exist
    :xx      only set if the key already exists

  `{:nx true :ex n}` is the atomic lock-acquisition idiom (SET NX EX).

  Options within a group are mutually exclusive (:ex / :px / :keepttl, and :nx / :xx); when more than one is given, the
  last builder call wins — pass at most one of each group."
  ([client ^String k v]
   (set client k v nil))
  ([client ^String k v {:keys [ex px nx xx keepttl]}]
   (let [c (->base-client client)
         gk (str->gs k)
         gv (bytes->gs (encode client v))]
     (if (or ex px nx xx keepttl)
       (let [b (SetOptions/builder)]
         (when ex (.expiry b (SetOptions$Expiry/Seconds (Long/valueOf (long ex)))))
         (when px (.expiry b (SetOptions$Expiry/Milliseconds (Long/valueOf (long px)))))
         (when keepttl (.expiry b (SetOptions$Expiry/KeepExisting)))
         (when nx (.conditionalSet b SetOptions$ConditionalSet/ONLY_IF_DOES_NOT_EXIST))
         (when xx (.conditionalSet b SetOptions$ConditionalSet/ONLY_IF_EXISTS))
         (fut-get (.set c gk gv (.build b))))
       (fut-get (.set c gk gv))))))

(defn setex
  "Sets key `k` to `v` with a TTL of `expire-sec` seconds. Shorthand for `(set client k v {:ex expire-sec})`."
  [client ^String k expire-sec v]
  (set client k v {:ex expire-sec}))

(defn del
  "Deletes the given keys. Returns the number of keys deleted; 0 without contacting the server when no keys are given.

  In cluster mode, multi-key handling is delegated to GLIDE."
  ^Long [client & ks]
  (if (seq ks)
    (fut-get (.del (->base-client client) (gs-array (mapv str->gs ks))))
    0))

(defn exists?
  "Returns true when key `k` exists."
  [client ^String k]
  (pos? ^Long (fut-get (.exists (->base-client client) (gs-array [(str->gs k)])))))

(defn incr
  ^Long [client ^String k]
  (fut-get (.incr (->base-client client) (str->gs k))))

(defn incrby
  ^Long [client ^String k n]
  (fut-get (.incrBy (->base-client client) (str->gs k) (long n))))

(defn decr
  ^Long [client ^String k]
  (fut-get (.decr (->base-client client) (str->gs k))))

(defn decrby
  ^Long [client ^String k n]
  (fut-get (.decrBy (->base-client client) (str->gs k) (long n))))

(defn expire
  "Sets the TTL of key `k` to `seconds`. Returns true when the timeout was set, false when the key does not exist."
  [client ^String k seconds]
  (fut-get (.expire (->base-client client) (str->gs k) (long seconds))))

(defn ttl
  "Returns the remaining TTL of key `k` in seconds; -2 when the key does not exist, -1 when it has no TTL."
  ^Long [client ^String k]
  (fut-get (.ttl (->base-client client) (str->gs k))))

(defn mget
  "Returns the values of `ks` in order (nil for missing keys), as a real MGET; [] without contacting the server when no
  keys are given. In cluster mode, multi-slot handling is delegated to GLIDE."
  [client ks]
  (if (seq ks)
    (let [c (->base-client client)
          ^objects res (fut-get (.mget c (gs-array (mapv str->gs ks))))]
      (mapv (fn [gs] (when gs (decode client (gs->bytes gs)))) res))
    []))

(defn keys
  "Returns the keys matching `pattern` as a vector of strings. In cluster mode, results from all nodes are concatenated.

  KEYS is O(N) over the whole keyspace — do not use it on production data paths; prefer `scan`."
  [client ^String pattern]
  (let [c (->base-client client)
        gp (str->gs pattern)]
    (if (instance? GlideClusterClient c)
      (let [^ClusterValue cv (fut-get (.keys ^GlideClusterClient c gp))]
        (if (.hasSingleData cv)
          (mapv gs->str (.getSingleValue cv))
          (into []
                (comp (mapcat val)
                      (map gs->str))
                (.getMultiValue cv))))
      (mapv gs->str (fut-get (.keys ^GlideClient c gp))))))

(def ^:private scan-batch-size 1000)

(defn- standalone-scan
  [^GlideClient client ^String pattern]
  (let [^ScanOptions opts (-> (ScanOptions/builder)
                              (.matchPattern pattern)
                              (.count (Long/valueOf (long scan-batch-size)))
                              (.build))]
    (loop [cursor "0"
           acc (transient [])]
      (let [^objects res (fut-get (.scan client cursor opts))
            next-cursor (str (aget res 0))
            ^objects keys-arr (aget res 1)
            acc' (reduce (fn [a k] (conj! a (str k))) acc keys-arr)]
        (if (= next-cursor "0")
          (persistent! acc')
          (recur next-cursor acc'))))))

(defn- cluster-scan
  ;; Cursor handles are released deterministically, following the pattern documented on ClusterScanCursor: release the
  ;; previous cursor after every scan call, and release the current one when a call throws. GLIDE would free unreleased
  ;; handles on garbage collection, but a long-lived process should not depend on that.
  [^GlideClusterClient client ^String pattern]
  (let [opts (-> (ScanOptions/builder)
                 (.matchPattern pattern)
                 (.count (Long/valueOf (long scan-batch-size)))
                 (.build))]
    (loop [^ClusterScanCursor cursor (ClusterScanCursor/initialCursor)
           acc (transient [])]
      (let [^objects res (try
                           (fut-get (.scan client cursor opts))
                           (catch Throwable t
                             (.releaseCursorHandle cursor)
                             (throw t)))
            ^ClusterScanCursor next-cursor (aget res 0)
            ^objects keys-arr (aget res 1)
            acc' (reduce (fn [a k] (conj! a (str k))) acc keys-arr)]
        (.releaseCursorHandle cursor)
        (if (.isFinished next-cursor)
          (do (.releaseCursorHandle next-cursor)
              (persistent! acc'))
          (recur next-cursor acc'))))))

(defn scan
  "Returns all keys matching `pattern` by iterating SCAN to completion, as a vector of strings. Scans all shards in
  cluster mode."
  [client ^String pattern]
  (let [c (->base-client client)]
    (if (instance? GlideClusterClient c)
      (cluster-scan c pattern)
      (standalone-scan c pattern))))

(defn- ->info-section
  ^InfoOptions$Section [k]
  (case k
    :server InfoOptions$Section/SERVER
    :clients InfoOptions$Section/CLIENTS
    :memory InfoOptions$Section/MEMORY
    :persistence InfoOptions$Section/PERSISTENCE
    :stats InfoOptions$Section/STATS
    :replication InfoOptions$Section/REPLICATION
    :cpu InfoOptions$Section/CPU
    :commandstats InfoOptions$Section/COMMANDSTATS
    :latencystats InfoOptions$Section/LATENCYSTATS
    :sentinel InfoOptions$Section/SENTINEL
    :cluster InfoOptions$Section/CLUSTER
    :modules InfoOptions$Section/MODULES
    :keyspace InfoOptions$Section/KEYSPACE
    :errorstats InfoOptions$Section/ERRORSTATS
    :all InfoOptions$Section/ALL
    :default InfoOptions$Section/DEFAULT
    :everything InfoOptions$Section/EVERYTHING))

(defn- info-section-array
  ^"[Lglide.api.models.commands.InfoOptions$Section;" [section]
  (into-array InfoOptions$Section [(->info-section section)]))

(defn info
  "Returns the INFO text. With no `section`, returns the DEFAULT sections.

  Standalone: returns a string. Cluster: returns a map of node address to that node's INFO string (a nil key for
  single, unrouted replies)."
  ([client]
   (info client nil))
  ([client section]
   (let [c (->base-client client)]
     (if (instance? GlideClusterClient c)
       (let [^ClusterValue cv (if section
                                (fut-get (.info ^GlideClusterClient c (info-section-array section)))
                                (fut-get (.info ^GlideClusterClient c)))]
         (if (.hasSingleData cv)
           {nil (.getSingleValue cv)}
           (into {} (.getMultiValue cv))))
       (if section
         (fut-get (.info ^GlideClient c (info-section-array section)))
         (fut-get (.info ^GlideClient c)))))))

;;; ---- component -------------------------------------------------------------

(defrecord Glide [config codec client]
  component/Lifecycle
  (start [this]
    (if client
      this
      (assoc this :client (create-client config))))
  (stop [this]
    (when client
      (.close ^BaseClient client))
    (assoc this :client nil))

  health/HealthCheck
  (-alive? [this]
    (try
      (= "PONG" (ping this))
      (catch Exception e
        (log/error e "Glide is dead")
        false))))
