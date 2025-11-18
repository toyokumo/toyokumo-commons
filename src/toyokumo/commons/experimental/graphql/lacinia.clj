(ns toyokumo.commons.experimental.graphql.lacinia
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [com.walmartlabs.lacinia.parser.schema :as l.parser.schema]
   [com.walmartlabs.lacinia.schema :as l.schema]
   [com.walmartlabs.lacinia.util :as l.util]))

(defrecord Lacinia [sdl-path enable-introspection? executor resolver compiled-schema]
  component/Lifecycle
  (start [this]
    (if-let [sdl (io/resource sdl-path)]
      (-> sdl
          slurp
          l.parser.schema/parse-schema
          (l.util/inject-resolvers (:resolvers resolver))
          (l.schema/compile (cond-> {:enable-introspection? (boolean enable-introspection?)}
                              executor (assoc :executor executor)))
          (->> (assoc this :compiled-schema)))
      (throw (IllegalArgumentException. (str "Schema Definition Language file can not find in " sdl-path)))))
  (stop [this]
    (assoc this :compiled-schema nil)))
