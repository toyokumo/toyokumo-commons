(ns toyokumo.commons.email-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.email :refer [satisfy-rfc-822? quote-email unquote-email]]))

(deftest satisfy-rfc-822?-test
  (are [x y] (= x y)
    true (satisfy-rfc-822? "foo@foo.com")
    true (satisfy-rfc-822? "foo@foocom")
    true (satisfy-rfc-822? "fo.o@foo.com")
    false (satisfy-rfc-822? ".foo@foo.com")
    false (satisfy-rfc-822? "fo..o@foo.com")
    false (satisfy-rfc-822? "foo.@foo.com")))

(deftest quote-email-test
  (are [x y] (= x y)
    "foo@foo.com" (quote-email "foo@foo.com")
    "foo@foocom" (quote-email "foo@foocom")
    "fo.o@foo.com" (quote-email "fo.o@foo.com")
    "\".foo\"@foo.com" (quote-email ".foo@foo.com")
    "\"fo..o\"@foo.com" (quote-email "fo..o@foo.com")
    "\"foo.\"@foo.com" (quote-email "foo.@foo.com")))

(deftest unquote-email-test
  (are [x y] (= x y)
    "foo@foo.com" (unquote-email "foo@foo.com")
    "foo@foocom" (unquote-email "foo@foocom")
    "fo.o@foo.com" (unquote-email "fo.o@foo.com")
    ".foo@foo.com" (unquote-email "\".foo\"@foo.com")
    "fo..o@foo.com" (unquote-email "\"fo..o\"@foo.com")
    "foo.@foo.com" (unquote-email "\"foo.\"@foo.com")))
