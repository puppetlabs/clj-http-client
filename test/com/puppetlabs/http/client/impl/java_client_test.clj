(ns com.puppetlabs.http.client.impl.java-client-test
  (:import (com.puppetlabs.http.client.impl JavaClient)
           (org.apache.commons.io IOUtils)
           (com.puppetlabs.http.client ResponseBodyType RequestOptions)
           (org.apache.http.entity ContentType)
           (java.io ByteArrayInputStream))
  (:require [clojure.test :refer :all]))

;; NOTE: there are more comprehensive, end-to-end tests for
;; the Java client functionality lumped in with the clojure
;; tests.  This namespace is just for some Java-only unit tests.

(deftest test-coerce-body-type
  (testing "Can handle a Content Type header with no charset"
    (let [body "foo"
          body-stream (IOUtils/toInputStream body "UTF-8")]
      (is (= "foo" (JavaClient/coerceBodyType
                     body-stream
                     ResponseBodyType/TEXT
                     ContentType/WILDCARD))))))

(defn request-options
  [body content-type-value]
  (new RequestOptions
       nil {"content-type" content-type-value} body false nil))

(defn compute-content-type
  [body content-type-value]
  (->
    (JavaClient/getContentType body (request-options body content-type-value))
    ;; Calling .toString on an instance of org.apache.http.entity.ContentType
    ;; generates the string that'll actually end up in the header.
    .toString))


;; This test case is 100% copypasta from puppetlabs.http.client.async-test
(deftest content-type-test
  (testing "value of content-type header is computed correctly"
    (testing "a byte stream which specifies application/octet-stream"
      (let [body (ByteArrayInputStream. (byte-array [(byte 1) (byte 2)]))]
        (is (= (compute-content-type body "application/octet-stream")
               "application/octet-stream"))))

    (testing "the request body is a string"
      (testing "when a charset is specified, it is honored"
        (let [body "foo"]
          (is (= (compute-content-type body "text/plain; charset=US-ASCII")
                 "text/plain; charset=US-ASCII"))))

      (testing "a missing charset yields a content-type that maintains
                the given mime-type but adds UTF-8 as the charset"
        (let [body "foo"]
          (is (= (compute-content-type body "text/html")
                 "text/html; charset=UTF-8")))))))


(deftest null-response-body-coerced-as-text
  (testing "a null response body is coerced into a string by JavaClient.coerceBodyType"
    (let [body nil]
      (is (= "" (JavaClient/coerceBodyType body ResponseBodyType/TEXT nil))))))
