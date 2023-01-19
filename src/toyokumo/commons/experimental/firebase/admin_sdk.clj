(ns toyokumo.commons.experimental.firebase.admin-sdk
  "Provides a component which initializes FirebasApp with the
  `com.stuartsierra.component/Component` lifecycle.

  service-account-key-path  - required. File path to the firebase service account key.
  options-builder           - optional. An instance of FirebaseOptions$Builder. Use to customize FirebaseApp initialization.

  See for more detail on Component at https://github.com/stuartsierra/component."
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
