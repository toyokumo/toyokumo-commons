(ns toyokumo.commons.email
  (:require
   [clojure.string :as str])
  (:import
   (javax.mail.internet
    AddressException
    InternetAddress)))

(defn satisfy-rfc-822?
  "true if the mail satisfies RFC 822"
  [^String email]
  (try
    (.validate (InternetAddress. email))
    true
    (catch AddressException _
      false)))

(defn quote-email
  "Quote local part of the mail if the email doesn't satisfy RFC 822"
  ^String [^String email]
  (if (satisfy-rfc-822? email)
    email
    (let [[local domain] (str/split email #"@")]
      (str "\"" local "\"@" domain))))

(defn unquote-email
  "Unquote email that is quoted by quote-email above"
  ^String [^String email]
  (str/replace email "\"" ""))
