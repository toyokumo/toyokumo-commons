# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Clojure utility library shared across Toyokumo products, published to Clojars as `toyokumo/toyokumo-commons`. Uses deps.edn (no Leiningen).

## Commands

```sh
make docker-up   # start PostgreSQL + Valkey (required for db/redis/valkey tests)
make test        # run all tests (clojure -M:dev:test, kaocha)
make lint        # cljstyle check + clj-kondo (src/ only)
make install     # install to local maven repo
make outdated    # antq dependency upgrade check
```

Run a single test namespace or var with kaocha's `--focus`:

```sh
clojure -M:dev:test --focus toyokumo.commons.csv-test
clojure -M:dev:test --focus toyokumo.commons.url-test/some-test
```

Kaocha is configured with `:fail-fast? true` (tests.edn), so a run stops at the first failure.

Fix formatting with `cljstyle fix` (config in `.cljstyle`).

### Test prerequisites

`db`/`postgresql` tests connect to localhost PostgreSQL (5432, user `toyokumo`, password `commons`, db `toyokumo`); `redis`/`valkey` tests connect to localhost Valkey (6379, RESP-compatible with Redis, so both carmine and glide tests run against it). Start them with `make docker-up` first; `test/helper.clj` creates and drops the test tables around each db test. CI runs the same services and tests against Java 17, 21, and 25.

## Architecture

Three source trees, all on the classpath:

- `src/` — JVM Clojure (most of the library: db, csv, email, ring, jetty9, redis, json, transit, ...)
- `src-cljc/` — cross-platform (`toyokumo.commons.core`, `toyokumo.commons.util`)
- `src-cljs/` — ClojureScript only (`toyokumo.commons.transit`)

Key patterns:

- **Stuart Sierra component lifecycle**: stateful things (`db.hikari-cp/HikariCP`, `server.jetty9/Jetty9Server`, `valkey.glide/Glide`, `redis.carmine/Carmine`, etc.) are defrecords implementing `com.stuartsierra.component/Lifecycle` with idempotent start/stop.
- **Health checks**: `toyokumo.commons.health` defines a `HealthCheck` protocol implemented by components; `ring.middleware.health` exposes it as an HTTP endpoint.
- **Valkey client** (`valkey.glide`): wraps the Valkey GLIDE Java SDK. Values go through a codec attached to the client (`valkey.glide.codec/default`, or `carmine-compat` for wire compatibility when migrating from carmine — see `doc/valkey-glide.md`). Commands not provided by the library are meant to be defined in product code from the public low-level building blocks (`->base-client`, `str->gs`, `fut-get`, `encode`/`decode`); don't grow the command set without need. `redis.carmine` is the legacy client, kept until products finish migrating.
- **Experimental namespaces**: `src/toyokumo/commons/experimental/` (lacinia GraphQL, superlifter, firebase) depend on libraries that are only in the `:dev` alias, not in the library's hard dependencies — consumers add those deps themselves. Keep new optional-dependency code under `experimental/` and its deps in `:dev`.
- **Schema validation**: prismatic/schema is used for fn schemas; tests enable validation via `helper/enable-validation-fixture`.

## Releasing

Version is `0.4.{{git/commit-count}}` (build.edn, liquidz/build.edn tooling). Add user-facing changes under `## [Unreleased]` in CHANGELOG.md; the release workflow (GitHub Actions `workflow_dispatch`) stamps the version heading, tags, and publishes.
