(ns build
  (:require
   [clojure.java.shell :as sh]
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as deploy]))

(def ^:private version (format "0.3.%s" (b/git-count-revs nil)))
(def ^:private lib 'toyokumo/toyokumo-commons)
(def ^:private class-dir "target/classes")
(def ^:private jar-file "target/toyokumo-commons.jar")
(def ^:private scm {:connection "scm:git:git://github.com/toyokumo/toyokumo-commons.git"
                    :developerConnection "scm:git:ssh://git@github.com/toyokumo/toyokumo-commons.git"
                    :url "https://github.com/toyokumo/toyokumo-commons"
                    :tag version})

(defn pom
  [_]
  (let [basis (b/create-basis)]
    (b/write-pom {:basis basis
                  :class-dir class-dir
                  :lib lib
                  :version version
                  :src-dirs (:paths basis)
                  :scm scm})))

(defn jar
  [arg]
  (let [basis (b/create-basis)]
    (pom arg)
    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})))

(defn install
  [arg]
  (jar arg)
  (deploy/deploy {:artifact jar-file
                  :installer :local
                  :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

(defn tag
  [_]
  (sh/sh "git" "tag" version)
  (sh/sh "git" "push" "origin" version))

(defn deploy
  [arg]
  (assert (and (System/getenv "CLOJARS_USERNAME")
               (System/getenv "CLOJARS_PASSWORD")))
  (jar arg)
  (deploy/deploy {:artifact jar-file
                  :installer :remote
                  :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))
