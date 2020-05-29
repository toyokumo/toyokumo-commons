(ns toyokumo.commons.ring.middleware.trailing-slash-test
  (:require
   [clojure.test :refer :all]
   [toyokumo.commons.ring.middleware.trailing-slash :refer [wrap-trailing-slash]]))

(deftest wrap-trailing-slash-test
  (let [handler (wrap-trailing-slash identity)]
    (is (= {:uri "/"
            :path-info "/"}
           (handler {:uri "/"
                     :path-info "/"})))
    (is (= {:uri "/foo"
            :path-info "/foo"}
           (handler {:uri "/foo"
                     :path-info "/foo"})))
    (is (= {:uri "/foo"
            :path-info "/foo"}
           (handler {:uri "/foo/"
                     :path-info "/foo/"})))))
