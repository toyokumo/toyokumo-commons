(ns toyokumo.commons.ring.response
  (:require
   [ring.util.http-response :as res]
   [toyokumo.commons.url :as tc.url]))

;;; Make response map

;; 20X
(def ok res/ok)
(def created res/created)
(def no-content res/no-content)

;; 30X
(def found res/found)
(def see-other res/see-other)
(def not-modified res/not-modified)

;; 40X
(def bad-request res/bad-request)
(def unauthorized res/unauthorized)
(def forbidden res/forbidden)
(def not-found res/not-found)
(def not-found! res/not-found!)
(def not-acceptable res/not-acceptable)
(def too-many-requests res/too-many-requests)

;; 50X
(def internal-server-error res/internal-server-error)
(def internal-server-error! res/internal-server-error!)

;;; Set headers

(def content-type res/content-type)

(def header res/header)

(defn content-disposition [resp value]
  (header resp "Content-Disposition" value))

(defn attachment
  "Use when you want to make a client save response as a file.
  For example:
   (let [csv-str \"foo, bar\"]
     (-> (ok csv-str)
         (attachment \"foobar.csv\")
         (csv)))"
  ([resp filename]
   (attachment resp filename "UTF-8"))
  ([resp filename charset]
   (content-disposition resp (format "attachment; filename=\"%s\"; filename*=%s''%s"
                                     filename charset (tc.url/url-encode filename charset)))))

;;; Specific Content-Type

(defn html [resp]
  (content-type resp "text/html"))

(defn json [resp]
  (content-type resp "application/json"))

(defn csv
  ([resp]
   (content-type resp "text/csv"))
  ([resp charset]
   (content-type resp (str "text/csv; charset=" charset))))
