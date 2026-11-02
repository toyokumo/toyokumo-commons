(ns toyokumo.commons.experimental.firebase.admin-sdk
  "Provides a component which initializes FirebasApp with the
  `com.stuartsierra.component/Component` lifecycle.

  service-account-key       - The firebase service account key as a JSON string.
                              Takes precedence over service-account-key-path when non-blank.
  service-account-key-path  - File path to the firebase service account key.
  options-builder           - optional. An instance of FirebaseOptions$Builder. Use to customize FirebaseApp initialization.

  One of service-account-key or service-account-key-path is required.

  See for more detail on Component at https://github.com/stuartsierra/component."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.stuartsierra.component :as component])
  (:import
   (com.google.auth.oauth2
    GoogleCredentials)
   (com.google.firebase
    FirebaseApp
    FirebaseOptions
    FirebaseOptions$Builder)
   (java.io
    ByteArrayInputStream
    InputStream)
   (java.nio.charset
    StandardCharsets)))

(defn- service-account-key-stream
  "Opens an InputStream over the service account key JSON, preferring a
  non-blank `service-account-key` over `service-account-key-path`."
  ^InputStream [service-account-key service-account-key-path]
  (cond
    (not (str/blank? service-account-key))
    (ByteArrayInputStream. (.getBytes ^String service-account-key StandardCharsets/UTF_8))

    (not (str/blank? service-account-key-path))
    (let [f (io/file service-account-key-path)]
      (when-not (.canRead f)
        (throw (IllegalArgumentException. "Firebase service account key file does not exist")))
      (io/input-stream f))

    :else
    (throw (IllegalArgumentException. "Either service-account-key or service-account-key-path is required"))))

(defrecord FirebaseAdmin [service-account-key service-account-key-path ^FirebaseApp app ^FirebaseOptions$Builder options-builder]
  component/Lifecycle
  (start [this]
    (with-open [is (service-account-key-stream service-account-key service-account-key-path)]
      (assoc this
             :app (let [opts (-> (or options-builder (FirebaseOptions/builder))
                                 (.setCredentials (GoogleCredentials/fromStream is))
                                 (.build))]
                    (FirebaseApp/initializeApp opts)))))
  (stop [this]
    (when app
      (.delete app))
    (assoc this :app nil)))
