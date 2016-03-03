(ns puppetlabs.http.client.async-plaintext-test
  (:import (com.puppetlabs.http.client Async RequestOptions ClientOptions)
           (org.apache.http.impl.nio.client HttpAsyncClients)
           (java.net URI SocketTimeoutException ServerSocket)
           (com.codahale.metrics MetricRegistry Timer)
           (com.puppetlabs.http.client.impl ClientMetricData))
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

(defn app-with-empty-content-type
  [_]
  {:headers {"content-type" ""}
   :status 200
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
        (testing "GET request via request function with persistent async client"
          (let [response (common/make-request client "http://localhost:10000/hello/" :get)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "Bad verb request via request function with persistent async client"
          (is (thrown? IllegalArgumentException
                       (common/make-request client
                                            "http://localhost:10000/hello/"
                                            :bad))))
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

(deftest empty-content-type-async
  (testing "content-type parsing handles empty content-type"
    (testlogging/with-test-logging
      (testwebserver/with-test-webserver app-with-empty-content-type port
         (let [url (str "http://localhost:" port "/hello")]
           (testing "java persistent async client"
             (with-open [client (-> (ClientOptions.)
                                    (Async/createClient))]
               (let [response (-> client
                                  (.get (RequestOptions. url))
                                  (.deref))]
                 (is (= 200 (.getStatus response))))))
           (testing "clojure persistent async client"
             (with-open [client (async/create-client {})]
               (let [response @(common/get client url {:as :text})]
                 (is (= 200 (:status response)))))))))))

(deftest metrics-test-java-async
  (testing "metrics work with java async client"
     (testlogging/with-test-logging
       (testutils/with-app-with-config
        app
        [jetty9/jetty9-service test-metric-web-service]
        {:webserver {:port 10000}}
         (let [metric-registry (MetricRegistry.)
               hello-request-opts (RequestOptions. "http://localhost:10000/hello")
               short-request-opts (RequestOptions. "http://localhost:10000/short")
               long-request-opts (RequestOptions. "http://localhost:10000/long")]
           (with-open [client (Async/createClient (ClientOptions.) metric-registry)]
             (-> client (.get hello-request-opts) (.deref)) ; warm it up
             (let [short-response (-> client (.get short-request-opts) (.deref))
                   long-response (-> client (.get long-request-opts) (.deref))]
               (-> client (.get short-request-opts) (.deref))
               (is (= 200 (.getStatus short-response)))
               (is (= "short" (slurp (.getBody short-response))))
               (is (= 200 (.getStatus long-response)))
               (is (= "long" (slurp (.getBody long-response))))
               (.timer metric-registry "fake")
               (let [client-metrics (.getClientMetrics client)
                     client-metrics-data (.getClientMetricsData client)
                     all-metrics (.getMetrics metric-registry)
                     short-id "puppetlabs.http-client.http://localhost:10000/short.GET"
                     long-id "puppetlabs.http-client.http://localhost:10000/long.GET"]
                 (testing ".getClientMetrics returns only http client metrics"
                   (is (= 4 (count all-metrics)))
                   (is (= 3 (count client-metrics)))
                   (is (= 3 (count client-metrics-data))))
                 (testing "get-client-metrics returns a map of metric name to timer instance"
                   (is (= (set (list "puppetlabs.http-client.http://localhost:10000/hello.GET"
                                     short-id long-id))
                          (set (keys client-metrics))
                          (set (keys client-metrics-data))))
                   (is (every? #(instance? Timer %) (vals client-metrics))))
                 (testing "get-client-metrics-data returns a map of metric id to metric data"
                   (let [short-data (get client-metrics-data short-id)
                         long-data (get client-metrics-data long-id)]
                     (is (every? #(instance? ClientMetricData %) (vals client-metrics-data)))
                     (is (= short-id (.getMetricId short-data)))
                     (is (= 2 (.getCount short-data)))
                     (is (<= 5 (.getMean short-data)))
                     (is (<= 10 (.getAggregate short-data)))

                     (is (= long-id (.getMetricId long-data)))
                     (is (= 1 (.getCount long-data)))
                     (is (<= 100 (.getMean long-data)))
                     (is (<= 100 (.getAggregate long-data)))

                     (is (> (.getAggregate long-data) (.getAggregate short-data))))))))
           (with-open [client (Async/createClient (ClientOptions.))]
             (testing ".getClientMetrics returns nil if no metrics registry passed in"
               (let [response (-> client (.get hello-request-opts) (.deref))]
                 (is (= 200 (.getStatus response)))
                 (is (= "Hello, World!" (slurp (.getBody response))))
                 (is (= nil (.getClientMetrics client)))
                 (is (= {} (.getClientMetricsData client)))))))))))

(deftest metrics-test-clojure-async
  (testing "metrics work with clojure async client"
    (testlogging/with-test-logging
      (testutils/with-app-with-config
        app
        [jetty9/jetty9-service test-metric-web-service]
        {:webserver {:port 10000}}
        (let [metric-registry (MetricRegistry.)]
          (with-open [client (async/create-client {} metric-registry)]
            @(common/get client "http://localhost:10000/hello") ; warm it up
            (let [short-response @(common/get client "http://localhost:10000/short" {:as :text})
                  long-response @(common/get client "http://localhost:10000/long")]
              @(common/get client "http://localhost:10000/short")
              (is (= {:status 200 :body "short"} (select-keys short-response [:status :body])))
              (is (= 200 (:status long-response)))
              (is (= "long" (slurp (:body long-response))))
              (.timer metric-registry "fake")
              (let [client-metrics (common/get-client-metrics client)
                    client-metrics-data (common/get-client-metrics-data client)
                    all-metrics (.getMetrics metric-registry)
                    short-id "puppetlabs.http-client.http://localhost:10000/short.GET"
                    long-id "puppetlabs.http-client.http://localhost:10000/long.GET"]
                (testing "get-client-metrics and get-client-metrics data return only http client metrics"
                  (is (= 4 (count all-metrics)))
                  (is (= 3 (count client-metrics)))
                  (is (= 3 (count client-metrics-data))))
                (testing "get-client-metrics returns a map of metric id to timer instance"
                  (is (= (set (list "puppetlabs.http-client.http://localhost:10000/hello.GET"
                                    short-id long-id))
                         (set (keys client-metrics))
                         (set (keys client-metrics-data))))
                  (is (every? #(instance? Timer %) (vals client-metrics))))
                (testing "get-client-metrics-data returns a map of metric id to metrics data"
                  (let [short-data (get client-metrics-data short-id)
                        long-data (get client-metrics-data long-id)]
                    (is (= short-id (:metric-id short-data)))
                    (is (= 2 (:count short-data)))
                    (is (<= 5 (:mean short-data)))
                    (is (<= 10 (:aggregate short-data)))

                    (is (= long-id (:metric-id long-data)))
                    (is (= 1 (:count long-data)))
                    (is (<= 100 (:mean long-data)))
                    (is (<= 100 (:aggregate long-data)))

                    (is (> (:mean long-data) (:mean short-data))))))))
          (with-open [client (async/create-client {})]
            (testing "get-client-metrics returns nil if no metrics registry passed in"
              (let [response (common/get client "http://localhost:10000/hello")]
                (is (= 200 (:status @response)))
                (is (= "Hello, World!" (slurp (:body @response))))
                (is (= nil (common/get-client-metrics client)))
                (is (= {} (common/get-client-metrics-data client)))))))))))
