(ns puppetlabs.http.client.async-unbuffered-test
  (:import (com.puppetlabs.http.client Async RequestOptions ClientOptions ResponseBodyType)
           (java.net SocketTimeoutException ConnectException)
           (java.io PipedInputStream PipedOutputStream)
           (java.util.concurrent TimeoutException)
           (java.util UUID))
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.test-common :refer :all]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.testutils.webserver :as testwebserver]
            [puppetlabs.http.client.common :as common]
            [puppetlabs.http.client.async :as async]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)

(defn- generate-data
  "Generate data of approximately the requested size, which is moderately compressible"
  [data-size]
  (apply str "xxxx" (repeatedly (/ data-size 35) #(UUID/randomUUID))))

(defn- successful-handler
  "A Ring handler that asynchronously sends some data, waits for confirmation the data has been received then sends
  some more data"
  [data send-more-data]
  (fn [_]
    (let [outstream (PipedOutputStream.)
          instream (PipedInputStream.)]
      (.connect instream outstream)
      ;; Return the response immediately and asynchronously stream some data into it
      (future
       (.write outstream (.getBytes data))
       ; Block until the client confirms it has read the first few bytes
       ; :socket-timeout-milliseconds on the client ensures we can't really get stuck here, even if the test fails
       (if send-more-data (deref send-more-data))
       ; Write the last of the data
       (.write outstream (.getBytes "yyyy"))
       (.close outstream))
      {:status 200
       :body instream})))

(defn- blocking-handler
  "A Ring handler that sends some data but then never closes the socket"
  [data]
  (fn [_]
    (let [outstream (PipedOutputStream.)
          instream (PipedInputStream.)]
      (.connect instream outstream)
      ;; Return the response immediately and asynchronously stream some data into it
      (future
       (.write outstream (.getBytes data)))
      {:status 200
       :body instream})))

(defn- clojure-non-blocking-streaming
  "Stream 32M of data (roughly) which is large enough to ensure the client won't buffer it all. Checks the data is
  streamed in a non-blocking manner i.e some data is received by the client before the server has finished
  transmission"
  [decompress-body?]
  (testlogging/with-test-logging
   (let [data (generate-data (* 32 1024 1024))
         opts {:as :unbuffered-stream :decompress-body decompress-body?}]

     (testing " - check data can be streamed successfully success"
       (let [send-more-data (promise)]
         (testwebserver/with-test-webserver-and-config
          (successful-handler data send-more-data) port {:shutdown-timeout-seconds 1}
          (with-open [client (async/create-client {:connect-timeout-milliseconds 100
                                                   :socket-timeout-milliseconds 20000})]
            (let [response @(common/get client (str "http://localhost:" port "/hello") opts)
                  {:keys [status body]} response]
              (is (= 200 status))
              (let [instream body
                    buf (make-array Byte/TYPE 4)
                    _ (.read instream buf)]
                (is (= "xxxx" (String. buf "UTF-8")))       ;; Make sure we can read a few chars off of the stream
                (deliver send-more-data true)               ;; Indicate we read some chars
                (is (= (str data "yyyy") (str "xxxx" (slurp instream)))))))))) ;; Read the rest and validate

     (testing " - check socket timeout is handled"
       (try
         (testwebserver/with-test-webserver-and-config
          (blocking-handler data) port {:shutdown-timeout-seconds 1}
          (with-open [client (async/create-client {:connect-timeout-milliseconds 100
                                                   :socket-timeout-milliseconds 200})]
            (let [response @(common/get client (str "http://localhost:" port "/hello") opts)
                  {:keys [body error]} response]
              (is (nil? error))
              ;; Consume the body to get the exception
              (is (thrown? SocketTimeoutException (slurp body))))))
         (catch TimeoutException e
           ;; Expected whenever a server-side failure is generated
           )))

     (testing " - check connection timeout is handled"
       (with-open [client (async/create-client {:connect-timeout-milliseconds 100})]
         (let [response @(common/get client (str "http://localhost:" 12345 "/bad") opts)
               {:keys [error]} response]
           (is error)
           (is (instance? ConnectException error))))))))

(deftest clojure-non-blocking-streaming-without-decompression
  (testing "clojure :unbuffered-stream with 32MB payload and no decompression"
    (clojure-non-blocking-streaming false)))

(deftest clojure-non-blocking-streaming-with-decompression
  (testing "clojure :unbuffered-stream with 32MB payload and decompression"
    (clojure-non-blocking-streaming true)))

(defn- clojure-blocking-streaming
  "Stream data that is buffered client-side i.e. in a blocking manner"
  [data opts]
  (testlogging/with-test-logging

   (testing " - check data can be streamed successfully"
     (testwebserver/with-test-webserver-and-config
      (successful-handler data nil) port {:shutdown-timeout-seconds 1}
      (with-open [client (async/create-client {:connect-timeout-milliseconds 100
                                               :socket-timeout-milliseconds 20000})]
        (let [response @(common/get client (str "http://localhost:" port "/hello") opts)
              {:keys [status body]} response]
          (is (= 200 status))
          (let [instream body
                buf (make-array Byte/TYPE 4)
                _ (.read instream buf)]
            (is (= "xxxx" (String. buf "UTF-8")))           ;; Make sure we can read a few chars off of the stream
            (is (= (str data "yyyy") (str "xxxx" (slurp instream))))))))) ;; Read the rest and validate

   (testing " - check socket timeout is handled"
     (try
       (testwebserver/with-test-webserver-and-config
        (blocking-handler data) port {:shutdown-timeout-seconds 1}
        (with-open [client (async/create-client {:connect-timeout-milliseconds 100
                                                 :socket-timeout-milliseconds 200})]
          (let [response @(common/get client (str "http://localhost:" port "/hello") opts)
                {:keys [error]} response]
            (is (instance? SocketTimeoutException error)))))
       (catch TimeoutException e
         ;; Expected whenever a server-side failure is generated
         )))

   (testing " - check connection timeout is handled"
     (with-open [client (async/create-client {:connect-timeout-milliseconds 100})]
       (let [response @(common/get client (str "http://localhost:" 12345 "/bad") opts)
             {:keys [error]} response]
         (is error)
         (is (instance? ConnectException error)))))))

(deftest clojure-blocking-streaming-without-decompression
  (testing "clojure :unbuffered-stream with 1K payload and no decompression"
    ;; This is a small enough payload that :unbuffered-stream still buffers it all in memory and so it behaves
    ;; identically to :stream
    (clojure-blocking-streaming (generate-data 1024) {:as :unbuffered-stream :decompress-body false})))

(deftest clojure-blocking-streaming-with-decompression
  (testing "clojure :unbuffered-stream with 1K payload and decompression"
    ;; This is a small enough payload that :unbuffered-stream still buffers it all in memory and so it behaves
    ;; identically to :stream
    (clojure-blocking-streaming (generate-data 1024) {:as :unbuffered-stream :decompress-body true})))

(deftest clojure-existing-streaming-with-small-payload-without-decompression
  (testing "clojure :stream with 1K payload and no decompression"
    (clojure-blocking-streaming (generate-data 1024) {:as :stream :decompress-body false})))

(deftest clojure-existing-streaming-with-small-payload-with-decompression
  (testing "clojure :stream with 1K payload and decompression"
    (clojure-blocking-streaming (generate-data 1024) {:as :stream :decompress-body true})))

(deftest clojure-existing-streaming-with-large-payload-without-decompression
  (testing "clojure :stream with 32M payload and no decompression"
    (clojure-blocking-streaming (generate-data (* 32 1024 1024)) {:as :stream :decompress-body false})))

(deftest clojure-existing-streaming-with-large-payload-with-decompression
  (testing "clojure :stream with 32M payload and decompression"
    (clojure-blocking-streaming (generate-data (* 32 1024 1024)) {:as :stream :decompress-body true})))

(defn- java-non-blocking-streaming
  "Stream 32M of data (roughly) which is large enough to ensure the client won't buffer it all. Checks the data is
  streamed in a non-blocking manner i.e some data is received by the client before the server has finished
  transmission"
  [decompress-body?]
  (testlogging/with-test-logging
    (let [data (generate-data (* 32 1024 1024))]

      (testing " - check data can be streamed successfully"
        (let [send-more-data (promise)]
          (testwebserver/with-test-webserver-and-config
            (successful-handler data send-more-data) port {:shutdown-timeout-seconds 1}
            (with-open [client (-> (ClientOptions.)
                                   (.setSocketTimeoutMilliseconds 20000)
                                   (.setConnectTimeoutMilliseconds 100)
                                   (Async/createClient))]
              (let [request-options (RequestOptions. (str "http://localhost:" port "/hello"))
                    _ (.setAs request-options ResponseBodyType/UNBUFFERED_STREAM)
                    _ (.setDecompressBody request-options decompress-body?)
                    response (-> client (.get request-options) .deref)
                    status (.getStatus response)
                    body (.getBody response)]
                (is (= 200 status))
                (let [instream body
                      buf (make-array Byte/TYPE 4)
                      _ (.read instream buf)]
                  (is (= "xxxx" (String. buf "UTF-8")))     ;; Make sure we can read a few chars off of the stream
                  (deliver send-more-data true)             ;; Indicate we read some chars
                  (is (= (str data "yyyy") (str "xxxx" (slurp instream)))))))))) ;; Read the rest and validate

      (testing " - check socket timeout is handled"
        (try
          (testwebserver/with-test-webserver-and-config
            (blocking-handler data) port {:shutdown-timeout-seconds 1}
            (with-open [client (-> (ClientOptions.)
                                   (.setSocketTimeoutMilliseconds 200)
                                   (.setConnectTimeoutMilliseconds 100)
                                   (Async/createClient))]
              (let [request-options (RequestOptions. (str "http://localhost:" port "/hello"))
                    _ (.setAs request-options ResponseBodyType/UNBUFFERED_STREAM)
                    _ (.setDecompressBody request-options decompress-body?)
                    response (-> client (.get request-options) .deref)
                    body (.getBody response)
                    error (.getError response)]
                (is (nil? error))
                ;; Consume the body to get the exception
                (is (thrown? SocketTimeoutException (slurp body))))))
          (catch TimeoutException e
            ;; Expected whenever a server-side failure is generated
            )))

      (testing " - check connection timeout is handled"
        (with-open [client (-> (ClientOptions.)
                               (.setConnectTimeoutMilliseconds 100)
                               (Async/createClient))]
          (let [request-options (RequestOptions. (str "http://localhost:" 12345 "/bad"))
                _ (.setAs request-options ResponseBodyType/UNBUFFERED_STREAM)
                _ (.setDecompressBody request-options decompress-body?)
                response (-> client (.get request-options) .deref)
                error (.getError response)]
            (is error)
            (is (instance? ConnectException error))))))))

(deftest java-non-blocking-streaming-without-decompression
  (testing "java :unbuffered-stream with 32MB payload and no decompression"
    (java-non-blocking-streaming false)))

(deftest java-non-blocking-streaming-with-decompression
  (testing "java :unbuffered-stream with 32MB payload and decompression"
    (java-non-blocking-streaming true)))

(defn- java-blocking-streaming
  "Stream data that is buffered client-side i.e. in a blocking manner"
  [data response-body-type decompress-body?]
  (testlogging/with-test-logging

    (testing " - check data can be streamed successfully success"
      (testwebserver/with-test-webserver-and-config
        (successful-handler data nil) port {:shutdown-timeout-seconds 1}
        (with-open [client (-> (ClientOptions.)
                               (.setSocketTimeoutMilliseconds 20000)
                               (.setConnectTimeoutMilliseconds 100)
                               (Async/createClient))]
          (let [request-options (RequestOptions. (str "http://localhost:" port "/hello"))
                _ (.setAs request-options response-body-type)
                _ (.setDecompressBody request-options decompress-body?)
                response (-> client (.get request-options) .deref)
                status (.getStatus response)
                body (.getBody response)]
            (is (= 200 status))
            (let [instream body
                  buf (make-array Byte/TYPE 4)
                  _ (.read instream buf)]
              (is (= "xxxx" (String. buf "UTF-8")))         ;; Make sure we can read a few chars off of the stream
              (is (= (str data "yyyy") (str "xxxx" (slurp instream))))))))) ;; Read the rest and validate

    (testing " - check socket timeout is handled"
      (try
        (testwebserver/with-test-webserver-and-config
          (blocking-handler data) port {:shutdown-timeout-seconds 1}
          (with-open [client (-> (ClientOptions.)
                                 (.setSocketTimeoutMilliseconds 200)
                                 (.setConnectTimeoutMilliseconds 100)
                                 (Async/createClient))]
            (let [request-options (RequestOptions. (str "http://localhost:" port "/hello"))
                  _ (.setAs request-options response-body-type)
                  _ (.setDecompressBody request-options decompress-body?)
                  response (-> client (.get request-options) .deref)
                  error (.getError response)]
              (is (instance? SocketTimeoutException error)))))
        (catch TimeoutException e
          ;; Expected whenever a server-side failure is generated
          )))

    (testing " - check connection timeout is handled"
      (with-open [client (-> (ClientOptions.)
                             (.setConnectTimeoutMilliseconds 100)
                             (Async/createClient))]
        (let [request-options (RequestOptions. (str "http://localhost:" 12345 "/bad"))
              _ (.setAs request-options response-body-type)
              _ (.setDecompressBody request-options decompress-body?)
              response (-> client (.get request-options) .deref)
              error (.getError response)]
          (is error)
          (is (instance? ConnectException error)))))))

(deftest java-blocking-streaming-without-decompression
  (testing "java :unbuffered-stream with 1K payload and no decompression"
    ;; This is a small enough payload that :unbuffered-stream still buffers it all in memory and so it behaves
    ;; identically to :stream
    (java-blocking-streaming (generate-data 1024) ResponseBodyType/UNBUFFERED_STREAM false)))

(deftest java-blocking-streaming-with-decompression
  (testing "java :unbuffered-stream with 1K payload and decompression"
    ;; This is a small enough payload that :unbuffered-stream still buffers it all in memory and so it behaves
    ;; identically to :stream
    (java-blocking-streaming (generate-data 1024) ResponseBodyType/UNBUFFERED_STREAM true)))

(deftest java-existing-streaming-with-small-payload-without-decompression
  (testing "java :stream with 1K payload and no decompression"
    (java-blocking-streaming (generate-data 1024) ResponseBodyType/STREAM false)))

(deftest java-existing-streaming-with-small-payload-with-decompression
  (testing "java :stream with 1K payload and decompression"
    (java-blocking-streaming (generate-data 1024) ResponseBodyType/STREAM false)))

(deftest java-existing-streaming-with-large-payload-without-decompression
  (testing "java :stream with 32M payload and no decompression"
    (java-blocking-streaming (generate-data (* 32 1024 1024)) ResponseBodyType/STREAM false)))

(deftest java-existing-streaming-with-large-payload-with-decompression
  (testing "java :stream with 32M payload and decompression"
    (java-blocking-streaming (generate-data (* 32 1024 1024)) ResponseBodyType/STREAM true)))
