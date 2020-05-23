(ns toyokumo.commons.health
  (:require
   [schema.core :as s]))

(defprotocol HealthCheck
  "Check status of the component"
  (-alive? [_]))

(s/defn alive? :- s/Bool
  [c :- (s/protocol HealthCheck)]
  (-alive? c))
