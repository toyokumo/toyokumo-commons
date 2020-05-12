(defproject toyokumo/toyokumo-commons "0.1.5"
  :description "Utility functions that are used in multiple TOYOKUMO products"
  :url "https://github.com/toyokumo/toyokumo-commons"
  :license {:name "Apache, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]
                        ["snapshots" :clojars]]
  :plugins [[lein-ancient "0.6.15"]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [prismatic/schema "1.1.12"]
                 [camel-snake-kebab "0.4.1"]
                 [cheshire "5.10.0"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [commons-codec/commons-codec "1.14"]
                 [metosin/ring-http-response "0.9.1"]
                 [org.apache.commons/commons-csv "1.8"]
                 [commons-io/commons-io "2.6"]]
  :source-paths ["src" "src-cljs" "src-cljc"]
  :repl-options {:init-ns toyokumo.commons.core})
