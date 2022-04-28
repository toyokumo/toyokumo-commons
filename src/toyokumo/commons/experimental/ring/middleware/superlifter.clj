(ns toyokumo.commons.experimental.ring.middleware.superlifter
  (:require
   [superlifter.api :as sl]))

(defn wrap-superlifter
  "Generate superlifter's context and add it to request map"
  ([handler]
   (wrap-superlifter handler nil))
  ([handler {:keys [:buckets :urania-opts]}]
   (let [buckets (merge {:default {:triggers {:interval {:interval 100}
                                              :queue-size {:threshold 1000}}}}
                        buckets)
         opts (cond-> {:buckets buckets}
                urania-opts (assoc :urania-opts urania-opts))]
     (fn [req]
       (let [context (sl/start! opts)]
         (try
           (handler (assoc req :superlifter context))
           (finally
             (sl/stop! context))))))))
