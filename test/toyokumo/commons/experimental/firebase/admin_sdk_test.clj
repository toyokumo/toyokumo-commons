(ns toyokumo.commons.experimental.firebase.admin-sdk-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [com.stuartsierra.component :as component]
   [jsonista.core :as json]
   [toyokumo.commons.experimental.firebase.admin-sdk :as sut])
  (:import
   (com.google.firebase
    FirebaseApp
    FirebaseOptions)
   (java.io
    File)
   (java.security
    KeyPairGenerator)
   (java.util
    Base64)))

(defn- delete-all-apps
  "The default FirebaseApp can be initialized only once per JVM, so remove
  whatever a previous test left behind."
  []
  (doseq [^FirebaseApp app (FirebaseApp/getApps)]
    (.delete app)))

(use-fixtures :each (fn [f]
                      (delete-all-apps)
                      (f)
                      (delete-all-apps)))

(defn- dummy-private-key-pem
  "A PKCS#8 PEM made from a throwaway RSA key, readable as the `private_key`
  of a service account key."
  []
  (let [key-pair (-> (doto (KeyPairGenerator/getInstance "RSA")
                       (.initialize 2048))
                     (.generateKeyPair))
        encoded (.encodeToString (Base64/getMimeEncoder 64 (.getBytes "\n"))
                                 (.getEncoded (.getPrivate key-pair)))]
    (str "-----BEGIN PRIVATE KEY-----\n" encoded "\n-----END PRIVATE KEY-----\n")))

(defn- dummy-service-account-json
  []
  (json/write-value-as-string {"type" "service_account"
                               "project_id" "dummy-project"
                               "private_key_id" "dummy-key-id"
                               "private_key" (dummy-private-key-pem)
                               "client_email" "dummy@dummy-project.iam.gserviceaccount.com"
                               "client_id" "000000000000000000000"
                               "token_uri" "https://oauth2.googleapis.com/token"}))

(defn- dummy-service-account-file
  "Writes a service account key JSON to a temporary file and returns its path."
  []
  (let [file (doto (File/createTempFile "firebase-adminsdk" ".json")
               (.deleteOnExit))]
    (spit file (dummy-service-account-json))
    (.getPath file)))

(defn- default-app-initialized?
  []
  (= 1 (count (FirebaseApp/getApps))))

(deftest firebase-admin-test
  (testing "initializes the default app from a JSON string"
    (let [started (component/start (sut/map->FirebaseAdmin {:service-account-key (dummy-service-account-json)}))]
      (is (instance? FirebaseApp (:app started)))
      (is (= (:app started) (FirebaseApp/getInstance)))
      (component/stop started)))

  (testing "initializes the default app from a file path"
    (let [started (component/start (sut/map->FirebaseAdmin {:service-account-key-path (dummy-service-account-file)}))]
      (is (instance? FirebaseApp (:app started)))
      (is (= (:app started) (FirebaseApp/getInstance)))
      (component/stop started)))

  (testing "a JSON string takes precedence over a file path"
    (let [started (component/start (sut/map->FirebaseAdmin {:service-account-key (dummy-service-account-json)
                                                            :service-account-key-path "/path/to/nowhere.json"}))]
      (is (default-app-initialized?))
      (component/stop started)))

  (testing "a blank JSON string falls back to the file path"
    (let [started (component/start (sut/map->FirebaseAdmin {:service-account-key ""
                                                            :service-account-key-path (dummy-service-account-file)}))]
      (is (default-app-initialized?))
      (component/stop started)))

  (testing "applies the given options builder"
    (let [started (component/start (sut/map->FirebaseAdmin {:service-account-key (dummy-service-account-json)
                                                            :options-builder (.setProjectId (FirebaseOptions/builder) "custom-project")}))]
      (is (= "custom-project" (.getProjectId (.getOptions ^FirebaseApp (:app started)))))
      (component/stop started)))

  (testing "fails to start when neither a JSON string nor a file path is given"
    (is (thrown-with-msg? IllegalArgumentException #"service-account-key"
          (component/start (sut/map->FirebaseAdmin {}))))
    (is (empty? (FirebaseApp/getApps))))

  (testing "fails to start when the file does not exist"
    (is (thrown-with-msg? IllegalArgumentException #"does not exist"
          (component/start (sut/map->FirebaseAdmin {:service-account-key-path "/path/to/nowhere.json"}))))
    (is (empty? (FirebaseApp/getApps))))

  (testing "stop deletes the default app"
    (let [stopped (component/stop (component/start (sut/map->FirebaseAdmin {:service-account-key (dummy-service-account-json)})))]
      (is (nil? (:app stopped)))
      (is (empty? (FirebaseApp/getApps))))))
