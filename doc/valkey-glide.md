# Valkey (GLIDE)

`toyokumo.commons.valkey.glide` is a Valkey client on top of the
[Valkey GLIDE](https://github.com/valkey-io/valkey-glide) Java SDK.
It supports standalone and cluster mode (including ElastiCache Serverless,
which requires the cluster protocol and TLS).

## Basic usage

```clojure
(require '[com.stuartsierra.component :as component]
         '[toyokumo.commons.valkey.glide :as glide])

(def system
  (component/system-map
   :valkey (glide/map->Glide {:config {:host "localhost" :port 6379}})
   :handler (component/using (handler-component) [:valkey])))

;; in the handler component:
(glide/set valkey "user:1" {:name "Taro"})
(glide/get valkey "user:1") ; => {:name "Taro"}
```

The `Glide` record implements `com.stuartsierra.component/Lifecycle` and
`toyokumo.commons.health/HealthCheck`, so it can be passed to
`toyokumo.commons.ring.middleware.health/wrap-health-check`.

Production (cluster + TLS + AZ-affinity reads):

```clojure
{:config {:host "..." :port 6379
          :use-tls? true
          :cluster-mode? true
          :read-from :az-affinity-replicas-and-primary
          :client-az "ap-northeast-1a"}}
```

`:read-from` is a per-client setting; create separate clients when some
reads must go to the primary.

## Configuration

The full config map accepted by `create-client` (and the `:config` of the
`Glide` record):

```clojure
{:host "localhost"                  ; default "localhost"
 :port 6379                         ; default 6379
 :password nil                      ; optional
 :username nil                      ; optional
 :use-tls? false                    ; default false
 :request-timeout-ms nil            ; optional; requests fail with a GLIDE
                                    ; TimeoutException when exceeded
 :database-id nil                   ; standalone only (cluster mode has no
                                    ; database selection)
 :read-from :primary                ; :primary / :prefer-replica / :az-affinity /
                                    ; :az-affinity-replicas-and-primary / :all-nodes
 :client-az nil                     ; availability zone for AZ affinity
 :cluster-mode? false               ; default false (= standalone)
 :remove-glide-shutdown-hook? true} ; default true (see below)
```

### GLIDE's JVM shutdown hook

GLIDE registers its own JVM shutdown hook that closes the client runtime.
Shutdown hooks run in parallel, so on SIGTERM that hook races with an
application's graceful shutdown (e.g. `Jetty9Server` draining in-flight
requests, which is the default in this library) and in-flight work that
touches Valkey fails with `ClosingException`.

Therefore the hook is removed once, on first client creation, by default.
This is safe: GLIDE's Java-side threads are daemon threads, so JVM exit
proceeds normally, and an orderly stop still closes the client via the
component's `stop`. Pass `:remove-glide-shutdown-hook? false` to keep
GLIDE's hook (e.g. in a short-lived tool that does not shut down
gracefully).

## Error handling

Commands throw GLIDE's own exception types (`glide.api.models.exceptions`);
they all extend `GlideException`, a `RuntimeException`:

- `RequestException` — the server returned an error (e.g. `INCR` on a
  non-numeric value)
- `TimeoutException` — `:request-timeout-ms` exceeded
- `ClosingException` — the client is closed; also thrown by `create-client`
  (and thus `component/start`) when the server is unreachable
- `ConnectionException` — connection problems on an established client

Although GLIDE's API is future-based, this wrapper blocks internally and
unwraps `java.util.concurrent.ExecutionException`, so callers catch the
types above directly:

```clojure
(try
  (glide/incr valkey "counter")
  (catch glide.api.models.exceptions.RequestException e
    ...))
```

This also applies to commands you define yourself, as long as they block
through `glide/fut-get` (see below).

## Commands

`toyokumo.commons.valkey.glide` provides: `get`, `set` (options `:ex`, `:px`,
`:nx`, `:xx`, `:keepttl`), `setex`, `del` (variadic), `exists?`, `incr`,
`incrby`, `decr`, `decrby`, `expire` (returns a boolean), `ttl`, `mget`,
`keys`, `scan` and `info`.

Every command accepts either a raw `BaseClient` (as returned by
`create-client`) or a map with a `:client` key, such as the `Glide` component
record — so commands work the same whether or not you use the component.

`keys` is O(N) over the whole keyspace — do not use it on production data
paths; prefer `scan`. Both scan all shards in cluster mode.

## Values and codecs

Values are encoded by the codec attached to the client (`:codec` on the
`Glide` record; the default is `toyokumo.commons.valkey.glide.codec/default`).

With the default codec, any nippy-serializable Clojure value can be stored,
and everything round-trips with full type fidelity — keywords, maps, vectors,
sets, nil, doubles, byte arrays — with one exception: integers are stored as
plain ASCII digits so that server-side `INCR`/`DECR` work, and therefore
decode as strings (`(glide/set c "n" 42)` → `(glide/get c "n")` returns `"42"`).

A codec is a plain map of `{:encode (fn [v] bytes), :decode (fn [bytes] v)}`,
so applications can supply their own.

## Defining commands not provided here

```clojure
(defn getdel [client k]
  (when-let [gs (glide/fut-get (.getdel (glide/->base-client client) (glide/str->gs k)))]
    (glide/decode client (glide/gs->bytes gs))))
```

## Ring session store

```clojure
(require '[toyokumo.commons.valkey.glide.ring.session :as session])

(ring.middleware.session/wrap-session
 handler
 {:store (session/glide-store valkey {:key-prefix "myapp:session"
                                       :expiration-secs 3600})})
```

`:key-prefix` is required; `:expiration-secs` defaults to 30 days. Session
values go through the client's codec, so with `carmine-compat` existing
sessions written by carmine's `carmine-store` remain readable.

## Migrating from carmine

1. Attach the carmine-compat codec: `(glide/map->Glide {:config {...} :codec codec/carmine-compat})`.
   With it, reads and writes are byte-identical to carmine, so old (carmine)
   and new (glide) instances can serve the same data during a rolling deploy.
2. Differences from carmine to check at call sites:
   - `expire` returns a boolean (not 1/0)
   - `info` returns a map keyed by node address in cluster mode
   - server errors are GLIDE exceptions (`ExecutionException` is unwrapped)
3. After all carmine instances are gone, you may switch to the default codec:
   it decodes everything carmine has written identically. Only the decoded
   type of keywords/doubles written *after* the switch changes (e.g. `:ja`
   starts coming back as `:ja` instead of `"ja"`); during a rolling deploy
   of the switch itself, instances may briefly observe that type
   difference. Byte arrays are unaffected — both codecs use carmine's
   `\0<` marker and round-trip as byte arrays. Most caches expire via TTL,
   so old-format data drains naturally.
