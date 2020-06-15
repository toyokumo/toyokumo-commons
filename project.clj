(defproject toyokumo/toyokumo-commons "0.1.6-SNAPSHOT"
  :description "Utility functions that are used in multiple TOYOKUMO products"
  :url "https://github.com/toyokumo/toyokumo-commons"
  :license {:name "Apache, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]
                        ["snapshots" :clojars]]
  :plugins [[lein-ancient "0.6.15"]
            [lein-nvd "1.4.0"]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [prismatic/schema "1.1.12"]
                 [camel-snake-kebab "0.4.1"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [commons-codec/commons-codec "1.14"]
                 [metosin/ring-http-response "0.9.1" :exclusions [ring/ring-core]]
                 [org.apache.commons/commons-csv "1.8"]
                 [commons-io/commons-io "2.6"]
                 [info.sunng/ring-jetty9-adapter "0.12.8"]
                 [com.stuartsierra/component "1.0.0"]
                 [hikari-cp "2.12.0"]
                 [seancorfield/next.jdbc "1.0.424"]
                 [metosin/jsonista "0.2.6"]
                 [com.taoensso/carmine "2.19.1"]
                 [org.clojure/tools.logging "1.1.0"]]
  :source-paths ["src" "src-cljs" "src-cljc"]
  :repl-options {:init-ns toyokumo.commons.core}
  :profiles {:dev {:dependencies [[clj-http "3.10.1"]
                                  [org.postgresql/postgresql "42.2.12"]]}})
