(defproject toyokumo/toyokumo-commons "0.2.5-SNAPSHOT"
  :description "Utility functions that are used in multiple TOYOKUMO products"
  :url "https://github.com/toyokumo/toyokumo-commons"
  :license {:name "Apache, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]
                        ["snapshots" :clojars]]
  :plugins [[lein-ancient "0.7.0"]
            [lein-nvd "1.4.1"]]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.5.648"]
                 [prismatic/schema "1.2.0"]
                 [camel-snake-kebab "0.4.2"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 [commons-codec/commons-codec "1.15"]
                 [metosin/ring-http-response "0.9.3" :exclusions [ring/ring-core]]
                 [org.apache.commons/commons-csv "1.9.0"]
                 [commons-io/commons-io "2.11.0"]
                 [info.sunng/ring-jetty9-adapter "0.17.6"]
                 [com.stuartsierra/component "1.1.0"]
                 [hikari-cp "2.14.0"]
                 [com.github.seancorfield/next.jdbc "1.2.772"]
                 [metosin/jsonista "0.3.5"]
                 [com.taoensso/carmine "3.1.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [com.sun.mail/jakarta.mail "2.0.1"]
                 [clj-http "3.12.3"]
                 [diehard "0.11.3"]]
  :source-paths ["src" "src-cljs" "src-cljc"]
  :repl-options {:init-ns toyokumo.commons.core}
  :profiles {:dev {:dependencies [[org.postgresql/postgresql "42.3.3"]]}})
