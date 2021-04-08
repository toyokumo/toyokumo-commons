(defproject toyokumo/toyokumo-commons "0.2.4-SNAPSHOT"
  :description "Utility functions that are used in multiple TOYOKUMO products"
  :url "https://github.com/toyokumo/toyokumo-commons"
  :license {:name "Apache, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]
                        ["snapshots" :clojars]]
  :plugins [[lein-ancient "0.7.0"]
            [lein-nvd "1.4.1"]]
  :dependencies [[org.clojure/clojure "1.10.3" ]
                 [org.clojure/core.async "1.3.610" :exclusions [org.clojure/tools.reader]]
                 [prismatic/schema "1.1.12"]
                 [camel-snake-kebab "0.4.2"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.cognitect/transit-cljs "0.8.264"]
                 [commons-codec/commons-codec "1.15"]
                 [metosin/ring-http-response "0.9.2" :exclusions [ring/ring-core]]
                 [org.apache.commons/commons-csv "1.8"]
                 [commons-io/commons-io "2.8.0"]
                 [info.sunng/ring-jetty9-adapter "0.14.3" :exclusions [org.eclipse.jetty/jetty-alpn-conscrypt-server
                                                                       org.conscrypt/conscrypt-openjdk-uber]]
                 [com.stuartsierra/component "1.0.0"]
                 [hikari-cp "2.13.0"]
                 [com.github.seancorfield/next.jdbc "1.1.646"]
                 [metosin/jsonista "0.3.1"]
                 [com.taoensso/carmine "3.1.0"]
                 [org.clojure/tools.logging "1.1.0"]
                 [com.sun.mail/jakarta.mail "2.0.0"]
                 [clj-http "3.12.1"]
                 [diehard "0.10.3"]]
  :source-paths ["src" "src-cljs" "src-cljc"]
  :repl-options {:init-ns toyokumo.commons.core}
  :profiles {:dev {:dependencies [[org.postgresql/postgresql "42.2.19"]]}})
