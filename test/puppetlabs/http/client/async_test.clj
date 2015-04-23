(ns puppetlabs.http.client.async-test
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.async :refer :all])
  (:import (java.io ByteArrayInputStream)))

(defn compute-content-type
  [body content-type-value]
  (->
    (content-type body {:headers {"content-type" content-type-value}})
    ;; Calling .toString on an instance of org.apache.http.entity.ContentType
    ;; generates the string that'll actually end up in the header.
    .toString))

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

(deftest nil-response-body-coerced-as-text
  (testing "a nil response body is coerced into a string by async/coerce-body-type"
    (let [resp {:body nil, :opts {:as :text}}]
      (is (= {:body "", :opts {:as :text}}
             (coerce-body-type resp))))))
