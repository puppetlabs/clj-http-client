(ns puppetlabs.http.client.decompress-test
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)
           (java.util.zip GZIPOutputStream DeflaterInputStream)
           (org.apache.commons.io IOUtils)
           (com.puppetlabs.http.client.impl JavaClient)
           (java.util HashMap))
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.async :as async]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)

(def compressible-body (apply str (repeat 1000 "f")))

(defn gzip
  [s]
  (let [baos (ByteArrayOutputStream.)
        gos  (GZIPOutputStream. baos)]
    (-> s
        (.getBytes "UTF-8")
        (ByteArrayInputStream.)
        (IOUtils/copy gos))
    (.close gos)
    (ByteArrayInputStream. (.toByteArray baos))))

(defn deflate
  [s]
  (-> s
      (.getBytes "UTF-8")
      (ByteArrayInputStream.)
      (DeflaterInputStream.)))

(deftest gzip-compress-test
  (testing "clojure gzip decompression"
    (let [test-response {:headers {"content-encoding" "gzip"}
                         :body    (gzip compressible-body)}
          response (async/decompress test-response)]
      (is (not (contains? (:headers response) "content-encoding")))
      (is (= compressible-body (slurp (:body response))))))
  (testing "java gzip decompression"
    (let [headers (HashMap. {"content-encoding" "gzip"})
          response (JavaClient/decompress (gzip compressible-body) headers)]
      (is (not (.containsKey headers "content-encoding")))
      (is (= compressible-body (slurp response))))))

(deftest deflate-compress-test
  (testing "clojure deflate decompression"
    (let [test-response {:headers {"content-encoding" "deflate"}
                         :body    (deflate compressible-body)}
          response (async/decompress test-response)]
      (is (not (contains? (:headers response) "content-encoding")))
      (is (= compressible-body (slurp (:body response))))))
  (testing "java gzip decompression"
    (let [headers (HashMap. {"content-encoding" "deflate"})
          response (JavaClient/decompress (deflate compressible-body) headers)]
      (is (not (.containsKey headers "content-encoding")))
      (is (= compressible-body (slurp response))))))