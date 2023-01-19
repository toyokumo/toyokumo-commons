(ns toyokumo.commons.experimental.firebase.admin-sdk
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component])
  (:import
   (com.google.auth.oauth2
    GoogleCredentials)
   (com.google.firebase
    FirebaseApp
    FirebaseOptions
    FirebaseOptions$Builder)))

(defrecord FirebaseAdmin [service-account-key-path ^FirebaseApp app ^FirebaseOptions$Builder options-builder]
  component/Lifecycle
  (start [this]
    (let [f (io/file service-account-key-path)]
      (when-not (.canRead f)
        (throw (IllegalArgumentException. "Firebase service account key file does not exist")))
      (with-open [is (io/input-stream f)]
        (assoc this
               :app (let [opts (-> (or options-builder (FirebaseOptions/builder))
                                   (.setCredentials (GoogleCredentials/fromStream is))
                                   (.build))]
                      (FirebaseApp/initializeApp opts))))))
  (stop [this]
    (when app
      (.delete app))
    (assoc this :app nil)))
