(ns toyokumo.commons.email
  (:require
   [clojure.string :as str]
   [schema.core :as s])
  (:import
   (javax.mail.internet
    AddressException
    InternetAddress)))

(s/defn satisfy-rfc-822? :- s/Bool
  "true if the mail satisfies RFC 822"
  [email :- s/Str]
  (try
    (.validate (InternetAddress. email))
    true
    (catch AddressException _
      false)))

(s/defn quote-email :- s/Str
  "Quote local part of the mail if the email doesn't satisfy RFC 822"
  [email :- s/Str]
  (if (satisfy-rfc-822? email)
    email
    (let [[local domain] (str/split email #"@")]
      (str "\"" local "\"@" domain))))

(s/defn unquote-email :- s/Str
  "Unquote email that is quoted by quote-email above"
  [email :- s/Str]
  (str/replace email "\"" ""))

(defprotocol Email
  (email [_ params] "Email synchronously")
  (email-async [_ params] "Email asynchronously and return a channel of core.async"))
