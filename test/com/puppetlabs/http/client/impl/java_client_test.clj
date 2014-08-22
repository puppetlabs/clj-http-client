(ns com.puppetlabs.http.client.impl.java-client-test
  (:import (com.puppetlabs.http.client.impl JavaClient)
           (org.apache.commons.io IOUtils)
           (com.puppetlabs.http.client ResponseBodyType)
           (org.apache.http.entity ContentType))
  (:require [clojure.test :refer :all]))

;; NOTE: there are more comprehensive, end-to-end tests for
;; the Java client functionality lumped in with the clojure
;; tests.  This namespace is just for some edge-casey,
;; Java-only unit tests.

(deftest test-coerce-body-type
  (testing "Can handle a Content Type header with no charset"
    (let [body "foo"
          body-stream (IOUtils/toInputStream body "UTF-8")]
      (is (= "foo" (JavaClient/coerceBodyType
                     body-stream
                     ResponseBodyType/TEXT
                     ContentType/WILDCARD))))))
