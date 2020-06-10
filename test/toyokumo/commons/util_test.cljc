(ns toyokumo.commons.util-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.util :refer [remove-trailing-slash]]))

(deftest remove-trailing-slash-test
  (are [x y] (= x y)
    "" (remove-trailing-slash "")
    "/" (remove-trailing-slash "/")
    "/foo" (remove-trailing-slash "/foo/")
    "/foo/bar" (remove-trailing-slash "/foo/bar")
    "/foo/bar" (remove-trailing-slash "/foo/bar/")))
