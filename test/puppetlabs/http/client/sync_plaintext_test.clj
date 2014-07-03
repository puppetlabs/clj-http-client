(ns puppetlabs.http.client.sync-plaintext-test
  (:import (com.puppetlabs.http.client SyncHttpClient RequestOptions
                                       HttpClientException ResponseBodyType)
           (javax.net.ssl SSLHandshakeException)
           (java.io ByteArrayInputStream InputStream)
           (java.nio.charset Charset))
  (:require [clojure.test :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.sync :as sync]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)

(defn app
  [req]
  {:status 200
   :body "Hello, World!"})

(tk/defservice test-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler app "/hello")
        context))

(defn basic-test
  [http-method java-method clj-fn]
  (testing (format "sync client: HTTP method: '%s'" http-method)
    (testlogging/with-test-logging
      (testutils/with-app-with-config app
        [jetty9/jetty9-service test-web-service]
        {:webserver {:port 10000}}
        (testing "java sync client"
          (let [options (RequestOptions. "http://localhost:10000/hello/")
                response (java-method options)]
            (is (= 200 (.getStatus response)))
            (is (= "Hello, World!" (slurp (.getBody response))))))
        (testing "clojure sync client"
          (let [response (clj-fn "http://localhost:10000/hello/")]
            (is (= 200 (:status response)))
            (is (= "Hello, World!" (slurp (:body response))))))))))

(deftest sync-client-head-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client"
        (let [options (RequestOptions. "http://localhost:10000/hello/")
              response (SyncHttpClient/head options)]
          (is (= 200 (.getStatus response)))
          (is (= nil (.getBody response)))))
      (testing "clojure sync client"
        (let [response (sync/head "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= nil (:body response))))))))

(deftest sync-client-get-test
  (basic-test "GET" #(SyncHttpClient/get %) sync/get))

(deftest sync-client-post-test
  (basic-test "POST" #(SyncHttpClient/post %) sync/post))

(deftest sync-client-put-test
  (basic-test "PUT" #(SyncHttpClient/put %) sync/put))

(deftest sync-client-delete-test
  (basic-test "DELETE" #(SyncHttpClient/delete %) sync/delete))

(deftest sync-client-trace-test
  (basic-test "TRACE" #(SyncHttpClient/trace %) sync/trace))

(deftest sync-client-options-test
  (basic-test "OPTIONS" #(SyncHttpClient/options %) sync/options))

(deftest sync-client-patch-test
  (basic-test "PATCH" #(SyncHttpClient/patch %) sync/patch))

(deftest sync-client-as-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client: :as unspecified"
        (let [options (RequestOptions. "http://localhost:10000/hello/")
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (instance? InputStream (.getBody response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "java sync client: :as :stream"
        (let [options (.. (RequestOptions. "http://localhost:10000/hello/")
                          (setAs ResponseBodyType/STREAM))
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (instance? InputStream (.getBody response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "java sync client: :as :text"
        (let [options (.. (RequestOptions. "http://localhost:10000/hello/")
                          (setAs ResponseBodyType/TEXT))
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (string? (.getBody response)))
          (is (= "Hello, World!" (.getBody response)))))
      (testing "clojure sync client: :as unspecified"
        (let [response (sync/get "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (instance? InputStream (:body response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "clojure sync client: :as :stream"
        (let [response (sync/get "http://localhost:10000/hello/" {:as :stream})]
          (is (= 200 (:status response)))
          (is (instance? InputStream (:body response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "clojure sync client: :as :text"
        (let [response (sync/get "http://localhost:10000/hello/" {:as :text})]
          (is (= 200 (:status response)))
          (is (string? (:body response)))
          (is (= "Hello, World!" (:body response))))))))

(defn header-app
  [req]
  (let [val (get-in req [:headers "fooheader"])]
    {:status  200
     :headers {"myrespheader" val}
     :body    val}))

(tk/defservice test-header-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
       (add-ring-handler header-app "/hello")
       context))

(deftest sync-client-request-headers-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config header-app
      [jetty9/jetty9-service test-header-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client"
        (let [options (-> (RequestOptions. "http://localhost:10000/hello/")
                          (.setHeaders {"fooheader" "foo"}))
              response (SyncHttpClient/post options)]
          (is (= 200 (.getStatus response)))
          (is (= "foo" (slurp (.getBody response))))
          (is (= "foo" (-> (.getHeaders response) (.get "myrespheader"))))))
      (testing "clojure sync client"
        (let [response (sync/post "http://localhost:10000/hello/" {:headers {"fooheader" "foo"}})]
          (is (= 200 (:status response)))
          (is (= "foo" (slurp (:body response))))
          (is (= "foo" (get-in response [:headers "myrespheader"]))))))))

(defn req-body-app
  [req]
  {:status  200
   :body    (slurp (:body req))})

(tk/defservice test-body-web-service
               [[:WebserverService add-ring-handler]]
               (init [this context]
                     (add-ring-handler req-body-app "/hello")
                     context))

(deftest sync-client-request-body-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config req-body-app
      [jetty9/jetty9-service test-body-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client: string body for post request"
        (let [options (-> (RequestOptions. "http://localhost:10000/hello/")
                          (.setBody "foo"))
              response (SyncHttpClient/post options)]
          (is (= 200 (.getStatus response)))
          (is (= "foo" (slurp (.getBody response)))))
        (let [options (-> (RequestOptions. "http://localhost:10000/hello/")
                          (.setBody (ByteArrayInputStream. (.getBytes "foo" "UTF-8"))))
              response (SyncHttpClient/post options)]
          (is (= 200 (.getStatus response)))
          (is (= "foo" (slurp (.getBody response))))))
      (testing "clojure sync client: string body for post request"
        (let [response (sync/post "http://localhost:10000/hello/" {:body (ByteArrayInputStream. (.getBytes "foo" "UTF-8"))})]
          (is (= 200 (:status response)))
          (is (= "foo" (slurp (:body response)))))))))

(def compressible-body (apply str (repeat 1000 "f")))

(defn compression-app
  [req]
  {:status  200
   :headers {"orig-accept-encoding" (get-in req [:headers "accept-encoding"])
             "content-type" "text/plain"
             "charset" "UTF-8"}
   :body    compressible-body})

(tk/defservice test-compression-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
       (add-ring-handler compression-app "/hello")
       context))

(defn test-compression
  [desc opts accept-encoding content-encoding content-should-match?]
  (testlogging/with-test-logging
    (testutils/with-app-with-config req-body-app
      [jetty9/jetty9-service test-compression-web-service]
      {:webserver {:port 10000}}
      (testing (str "java sync client: compression headers / response: " desc)
        (let [java-opts (cond-> (RequestOptions. "http://localhost:10000/hello/")
                                (contains? opts :decompress-body) (.setDecompressBody (:decompress-body opts))
                                (contains? opts :headers) (.setHeaders (:headers opts)))
              response (SyncHttpClient/get java-opts)]
          (is (= 200 (.getStatus response)))
          (is (= accept-encoding (.. response getHeaders (get "orig-accept-encoding"))))
          (is (= content-encoding (.. response getOrigContentEncoding)))
          (if content-should-match?
            (is (= compressible-body (slurp (.getBody response))))
            (is (not= compressible-body (slurp (.getBody response)))))))
      (testing (str "clojure sync client: compression headers / response: " desc)
        (let [response (sync/post "http://localhost:10000/hello/" opts)]
          (is (= 200 (:status response)))
          (is (= accept-encoding (get-in response [:headers "orig-accept-encoding"])))
          (is (= content-encoding (:orig-content-encoding response)))
          (if content-should-match?
            (is (= compressible-body (slurp (:body response))))
            (is (not= compressible-body (slurp (:body response))))))))))

(deftest sync-client-compression-test
  (test-compression "default" {} "gzip, deflate" "gzip" true))

(deftest sync-client-compression-gzip-test
  (test-compression "explicit gzip" {:headers {"accept-encoding" "gzip"}} "gzip" "gzip" true))

(deftest sync-client-compression-disabled-test
  (test-compression "explicit disable" {:decompress-body false} nil nil true))

(deftest sync-client-decompression-disabled-test
  (test-compression "explicit disable" {:headers {"accept-encoding" "gzip"}
                                        :decompress-body false} "gzip" "gzip" false))
