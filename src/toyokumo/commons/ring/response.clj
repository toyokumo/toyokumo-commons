(ns toyokumo.commons.ring.response
  (:require
   [clojure.string :as str]
   [ring.util.http-response :as res]
   [toyokumo.commons.url :as tc.url]))

;;; Make response map

;; 20X
(def ok res/ok)
(def created res/created)
(def accepted res/accepted)
(def no-content res/no-content)

;; 30X
(def moved-permanently res/moved-permanently)
(def found res/found)
(def see-other res/see-other)
(def not-modified res/not-modified)
(def temporary-redirect res/temporary-redirect)
(def permanent-redirect res/permanent-redirect)

;; 40X
(def bad-request res/bad-request)
(def unauthorized res/unauthorized)
(def forbidden res/forbidden)
(def not-found res/not-found)
(def not-found! res/not-found!)
(def not-acceptable res/not-acceptable)
(def conflict res/conflict)
(def gone res/gone)
(def too-many-requests res/too-many-requests)

;; 50X
(def internal-server-error res/internal-server-error)
(def internal-server-error! res/internal-server-error!)

;;; Set headers

(def content-type res/content-type)

(def header res/header)

(defn content-disposition [resp value]
  (header resp "Content-Disposition" value))

(defn- encode-filename [filename]
  (-> filename
      (tc.url/url-encode)
      (str/replace "+" " ")))

(defn attachment
  "Use when you want to make a client save response as a file.
  For example:
   (let [csv-str \"foo, bar\"]
     (-> (ok csv-str)
         (attachment \"foobar.csv\")
         (csv)))

  NOTE: Safari does not recognize correct filename when Content-Disposition does not contain filename*.
        For other browsers, filename* is not required."
  [resp filename]
  (let [encoded-filename (encode-filename filename)]
    (content-disposition resp (str "attachment; filename=\"" encoded-filename "\"; filename*=UTF-8''" encoded-filename))))

;;; Specific Content-Type

(defn html
  ([resp]
   (content-type resp "text/html"))
  ([resp charset]
   (content-type resp (str "text/html; charset=" charset))))

(defn json
  ([resp]
   (content-type resp "application/json"))
  ([resp charset]
   (content-type resp (str "application/json; charset=" charset))))

(defn csv
  ([resp]
   (content-type resp "text/csv"))
  ([resp charset]
   (content-type resp (str "text/csv; charset=" charset))))

;;; Write session

(defn session
  "Write session. See ring.middleware.session"
  ([resp m]
   (session resp m false))
  ([resp m recreate?]
   (assoc resp :session (if recreate?
                          (vary-meta m assoc :recreate true)
                          m))))

(defn flash
  "Add flash data. See ring.middleware.flash/wrap-flash"
  [resp m]
  (assoc resp :flash m))

(defn flash-success-message [resp msg]
  (assoc-in resp [:flash :message] {:status :success :msg msg}))

(defn flash-error-message [resp msg]
  (assoc-in resp [:flash :message] {:status :error :msg msg}))
