(ns puppetlabs.http.client.async-plaintext-test
  (:import (com.puppetlabs.http.client Async RequestOptions ClientOptions ResponseBodyType)
           (org.apache.http.impl.nio.client HttpAsyncClients)
           (java.net URI SocketTimeoutException ServerSocket ConnectException)
           (java.io PipedInputStream PipedOutputStream)
           (java.util.concurrent TimeoutException)
           (java.util UUID))
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.test-common :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.testutils.webserver :as testwebserver]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.common :as common]
            [puppetlabs.http.client.async :as async]
            [schema.test :as schema-test]))

(use-fixtures :once schema-test/validate-schemas)

(defn app
  [_]
  {:status 200
   :body "Hello, World!"})

(tk/defservice test-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler app "/hello")
        context))

(deftest persistent-async-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
    [jetty9/jetty9-service test-web-service]
    {:webserver {:port 10000}}
    (testing "java async client"
      (let [request-options (RequestOptions. (URI. "http://localhost:10000/hello/"))
            client-options (ClientOptions.)
            client (Async/createClient client-options)]
        (testing "HEAD request with persistent async client"
          (let [response (.head client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= nil (.getBody (.deref response))))))
        (testing "GET request with persistent async client"
          (let [response (.get client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "POST request with persistent async client"
          (let [response (.post client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "PUT request with persistent async client"
          (let [response (.put client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "DELETE request with persistent async client"
          (let [response (.delete client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "TRACE request with persistent async client"
          (let [response (.trace client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "OPTIONS request with persistent async client"
          (let [response (.options client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "PATCH request with persistent async client"
          (let [response (.patch client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "client closes properly"
          (.close client)
          (is (thrown? IllegalStateException
                       (.get client request-options))))))
    (testing "clojure async client"
      (let [client (async/create-client {})]
        (testing "HEAD request with persistent async client"
          (let [response (common/head client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= nil (:body @response)))))
        (testing "GET request with persistent async client"
          (let [response (common/get client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "POST request with persistent async client"
          (let [response (common/post client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "PUT request with persistent async client"
          (let [response (common/put client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "DELETE request with persistent async client"
          (let [response (common/delete client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "TRACE request with persistent async client"
          (let [response (common/trace client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "OPTIONS request with persistent async client"
          (let [response (common/options client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "PATCH request with persistent async client"
          (let [response (common/patch client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "client closes properly"
          (common/close client)
          (is (thrown? IllegalStateException
                       (common/get client
                                   "http://localhost:10000/hello/")))))))))

(deftest request-with-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (let [client (HttpAsyncClients/createDefault)
            opts   {:method :get :url "http://localhost:10000/hello/"}]
        (.start client)
        (testing "GET request works with request-with-client"
          (let [response (async/request-with-client opts nil client)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "Client persists when passed to request-with-client"
          (let [response (async/request-with-client opts nil client)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (.close client)))))

(deftest query-params-test-async
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-params-web-service]
      {:webserver {:port 8080}}
        (testing "URL Query Parameters work with the Java client"
          (let [client (Async/createClient (ClientOptions.))]
            (try
              (let [request-options (RequestOptions.
                                      (URI. "http://localhost:8080/params?foo=bar&baz=lux"))
                    response        (.get client request-options)]
                (is (= 200 (.getStatus (.deref response))))
                (is (= queryparams (read-string (slurp (.getBody
                                                         (.deref response)))))))
              (finally
                (.close client)))))
        (testing "URL Query Parameters work with the clojure client"
          (with-open [client (async/create-client {})]
            (let [opts     {:method       :get
                            :url          "http://localhost:8080/params/"
                            :query-params queryparams
                            :as           :text}
                  response (common/get client "http://localhost:8080/params" opts)]
                (is (= 200 (:status @response)))
                (is (= queryparams (read-string (:body @response)))))))
        (testing "URL Query Parameters can be set directly in the URL"
          (with-open [client (async/create-client {})]
            (let [response (common/get client
                                       "http://localhost:8080/params?paramone=one"
                                       {:as :text})]
              (is (= 200 (:status @response)))
              (is (= (str {"paramone" "one"}) (:body @response))))))
        (testing (str "URL Query Parameters set in URL are overwritten if params "
                      "are also specified in options map")
          (with-open [client (async/create-client {})]
            (let [response (common/get client
                                       "http://localhost:8080/params?paramone=one&foo=lux"
                                       query-options)]
              (is (= 200 (:status @response)))
              (is (= queryparams (read-string (:body @response))))))))))

(deftest redirect-test-async
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service redirect-web-service]
      {:webserver {:port 8080}}
      (testing (str "redirects on POST not followed by persistent Java client "
                    "when forceRedirects option not set to true")
        (let [client (Async/createClient (ClientOptions.))]
          (try
            (let [request-options  (RequestOptions.
                                     (URI. "http://localhost:8080/hello"))
                  response         (.post client request-options)]
              (is (= 302 (.getStatus (.deref response)))))
            (finally
              (.close client)))))
      (testing "redirects on POST followed by Java client when option is set"
        (let [client (Async/createClient (.. (ClientOptions.)
                                             (setForceRedirects true)))]
          (try
            (let [request-options (RequestOptions.
                                    (URI. "http://localhost:8080/hello"))
                  response        (.post client request-options)]
              (is (= 200 (.getStatus (.deref response))))
              (is (= "Hello, World!" (slurp (.getBody (.deref response))))))
            (finally
              (.close client)))))
      (testing "redirects not followed by Java client when :follow-redirects is false"
        (let [client (Async/createClient (.. (ClientOptions.)
                                             (setFollowRedirects false)))]
          (try
            (let [request-options (RequestOptions.
                                    (URI. "http://localhost:8080/hello"))
                  response        (.get client request-options)]
              (is (= 302 (.getStatus (.deref response)))))
            (finally
              (.close client)))))
      (testing ":follow-redirects overrides :force-redirects for Java client"
        (let [client (Async/createClient (.. (ClientOptions.)
                                             (setFollowRedirects false)
                                             (setForceRedirects true)))]
          (try
            (let [request-options (RequestOptions.
                                    (URI. "http://localhost:8080/hello"))
                  response        (.get client request-options)]
              (is (= 302 (.getStatus (.deref response)))))
            (finally
              (.close client)))))
      (testing (str "redirects on POST not followed by clojure client "
                    "when :force-redirects is not set to true")
        (with-open [client (async/create-client {:force-redirects false})]
          (let [opts     {:method :post
                          :url    "http://localhost:8080/hello"
                          :as     :text}
                response (common/post client "http://localhost:8080/hello" opts)]
            (is (= 302 (:status @response))))))
      (testing (str "redirects on POST followed by persistent clojure client "
                    "when option is set")
        (with-open [client (async/create-client {:force-redirects true})]
          (let [response (common/post client
                                      "http://localhost:8080/hello"
                                      {:as :text})]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (:body @response))))))
      (testing (str "persistent clojure client does not follow redirects when "
                    ":follow-redirects is set to false")
        (with-open [client (async/create-client {:follow-redirects false})]
          (let [response (common/get client
                                     "http://localhost:8080/hello"
                                     {:as :text})]
            (is (= 302 (:status @response))))))
      (testing ":follow-redirects overrides :force-redirects with persistent clj client"
        (with-open [client (async/create-client {:follow-redirects false
                                                 :force-redirects true})]
          (let [response (common/get client
                                     "http://localhost:8080/hello"
                                     {:as :text})]
            (is (= 302 (:status @response)))))))))

(deftest short-connect-timeout-persistent-java-test-async
  (testing (str "connection times out properly for java persistent client "
                "async request with short timeout")
    (with-open [client (-> (ClientOptions.)
                           (.setConnectTimeoutMilliseconds 250)
                           (Async/createClient))]
      (let [request-options     (RequestOptions. "http://127.0.0.255:65535")
            time-before-connect (System/currentTimeMillis)]
        (is (connect-exception-thrown? (-> client
                                           (.get request-options)
                                           (.deref)
                                           (.getError)))
            "Unexpected result for connection attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Connection attempt took significantly longer than timeout")))))

(deftest short-connect-timeout-persistent-clojure-test-async
  (testing (str "connection times out properly for clojure persistent client "
                "async request with short timeout")
    (with-open [client (async/create-client
                         {:connect-timeout-milliseconds 250})]
      (let [time-before-connect (System/currentTimeMillis)]
        (is (connect-exception-thrown? (-> @(common/get
                                              client
                                              "http://127.0.0.255:65535")
                                           :error))
            "Unexpected result for connection attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Connection attempt took significantly longer than timeout")))))

(deftest longer-connect-timeout-test-async
  (testing "connection succeeds for async request with longer connect timeout"
    (testlogging/with-test-logging
      (testwebserver/with-test-webserver app port
        (let [url (str "http://localhost:" port "/hello")]
          (testing "java persistent async client"
            (with-open [client (-> (ClientOptions.)
                                   (.setConnectTimeoutMilliseconds 2000)
                                   (Async/createClient))]
              (let [response (-> client
                                 (.get (RequestOptions. url))
                                 (.deref))]
                (is (= 200 (.getStatus response)))
                (is (= "Hello, World!" (slurp (.getBody response)))))))
          (testing "clojure persistent async client"
            (with-open [client (async/create-client
                                 {:connect-timeout-milliseconds 2000})]
              (let [response @(common/get client url {:as :text})]
                (is (= 200 (:status response)))
                (is (= "Hello, World!" (:body response)))))))))))

(deftest short-socket-timeout-persistent-java-test-async
  (testing (str "socket read times out properly for persistent java async "
                "request with short timeout")
    (with-open [client (-> (ClientOptions.)
                           (.setSocketTimeoutMilliseconds 1)
                           (Async/createClient))
                server (ServerSocket. 0)]
      (let [request-options     (-> "http://127.0.0.1:"
                                    (str (.getLocalPort server))
                                    (RequestOptions.))
            time-before-connect (System/currentTimeMillis)]
        (is (instance? SocketTimeoutException (-> client
                                                  (.get request-options)
                                                  (.deref)
                                                  (.getError)))
            "Unexpected result for get attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Get attempt took significantly longer than timeout")))))

(deftest short-socket-timeout-persistent-clojure-test-async
  (testing (str "socket read times out properly for clojure persistent client "
                "async request with short timeout")
    (with-open [client (async/create-client
                         {:socket-timeout-milliseconds 250})
                server (ServerSocket. 0)]
      (let [url                 (str "http://127.0.0.1:" (.getLocalPort server))
            time-before-connect (System/currentTimeMillis)]
        (is (instance? SocketTimeoutException
                       (-> @(common/get client url)
                           :error))
            "Unexpected result for get attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Get attempt took significantly longer than timeout")))))

(deftest longer-socket-timeout-test-async
  (testing "get succeeds for async request with longer socket timeout"
    (testlogging/with-test-logging
      (testwebserver/with-test-webserver app port
        (let [url (str "http://localhost:" port "/hello")]
          (testing "java persistent async client"
            (with-open [client (-> (ClientOptions.)
                                   (.setSocketTimeoutMilliseconds 2000)
                                   (Async/createClient))]
              (let [response (-> client
                                 (.get (RequestOptions. url))
                                 (.deref))]
                (is (= 200 (.getStatus response)))
                (is (= "Hello, World!" (slurp (.getBody response)))))))
          (testing "clojure persistent async client"
            (with-open [client (async/create-client
                                 {:socket-timeout-milliseconds 2000})]
              (let [response @(common/get client url {:as :text})]
                (is (= 200 (:status response)))
                (is (= "Hello, World!" (:body response)))))))))))

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

   (testing " - check data can be streamed successfully success"
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
    ;; This is a small enough payload that :unbuffered-stream still buffered it all in memory and so it behaves
    ;; identically to :stream
    (clojure-blocking-streaming (generate-data 1024) {:as :unbuffered-stream :decompress-body false})))

(deftest clojure-blocking-streaming-with-decompression
  (testing "clojure :unbuffered-stream with 1K payload and decompression"
    ;; This is a small enough payload that :unbuffered-stream still buffered it all in memory and so it behaves
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
