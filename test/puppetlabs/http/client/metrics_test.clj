(ns puppetlabs.http.client.metrics-test
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.async-unbuffered-test :as unbuffered-test]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.testutils.webserver :as testwebserver]
            [puppetlabs.http.client.async :as async]
            [puppetlabs.http.client.sync :as sync]
            [puppetlabs.http.client.common :as common]
            [puppetlabs.trapperkeeper.core :as tk])
  (:import (com.puppetlabs.http.client.impl ClientMetricData)
           (com.puppetlabs.http.client Async RequestOptions ClientOptions ResponseBodyType Sync)
           (com.codahale.metrics Timer MetricRegistry)
           (java.net SocketTimeoutException)
           (java.util.concurrent TimeoutException)))

(tk/defservice test-metric-web-service
               [[:WebserverService add-ring-handler]]
               (init [this context]
                     (add-ring-handler (fn [_] {:status 200 :body "Hello, World!"}) "/hello")
                     (add-ring-handler (fn [_]
                                         (do
                                           (Thread/sleep 5)
                                           {:status 200 :body "short"}))
                                       "/short")
                     (add-ring-handler (fn [_]
                                         (do
                                           (Thread/sleep 100)
                                           {:status 200 :body "long"}))
                                       "/long")
                     context))

(def hello-url "http://localhost:10000/hello")
(def short-url "http://localhost:10000/short")
(def long-url "http://localhost:10000/long")

(def short-name-base "puppetlabs.http-client.experimental.with-url.http://localhost:10000/short")
(def short-name (str short-name-base ".bytes-read"))
(def short-name-with-get (str short-name-base ".GET" ".bytes-read"))
(def short-name-with-post (str short-name-base ".POST" ".bytes-read"))

(def long-name-base "puppetlabs.http-client.experimental.with-url.http://localhost:10000/long")
(def long-name (str long-name-base ".bytes-read"))
(def long-name-with-method (str long-name-base ".GET" ".bytes-read"))
(def long-foo-name "puppetlabs.http-client.experimental.with-metric-id.foo.bytes-read")
(def long-foo-bar-name "puppetlabs.http-client.experimental.with-metric-id.foo.bar.bytes-read")
(def long-foo-bar-baz-name "puppetlabs.http-client.experimental.with-metric-id.foo.bar.baz.bytes-read")

(def hello-name-base "puppetlabs.http-client.experimental.with-url.http://localhost:10000/hello")
(def hello-name (str hello-name-base ".bytes-read"))
(def hello-name-with-method (str hello-name-base ".GET" ".bytes-read"))

(deftest metrics-test-java-async
  (testing "metrics work with java async client"
     (testlogging/with-test-logging
      (testutils/with-app-with-config
       app
       [jetty9/jetty9-service test-metric-web-service]
       {:webserver {:port 10000}}
       (let [metric-registry (MetricRegistry.)
             hello-request-opts (RequestOptions. hello-url)
             short-request-opts (RequestOptions. short-url)
             long-request-opts (doto (RequestOptions. long-url)
                                 (.setMetricId (into-array ["foo" "bar" "baz"])))]
         (with-open [client (Async/createClient (doto (ClientOptions.)
                                                  (.setMetricRegistry metric-registry)))]
           (-> client (.get hello-request-opts) (.deref)) ; warm it up
           (let [short-response (-> client (.get short-request-opts) (.deref))
                 long-response (-> client (.get long-request-opts) (.deref))]
             (-> client (.post short-request-opts) (.deref))
             (is (= 200 (.getStatus short-response)))
             (is (= "short" (slurp (.getBody short-response))))
             (is (= 200 (.getStatus long-response)))
             (is (= "long" (slurp (.getBody long-response))))
             (.timer metric-registry "fake")
             (let [client-metrics (.getClientMetrics client)
                   client-metrics-data (.getClientMetricsData client)
                   all-metrics (.getMetrics metric-registry)]
               (testing ".getClientMetrics returns only http client metrics"
                 (is (= 11 (count all-metrics)))
                 (is (= 10 (count client-metrics)))
                 (is (= 10 (count client-metrics-data))))
               (testing "get-client-metrics returns a map of metric name to timer instance"
                        (is (= (set (list hello-name hello-name-with-method short-name short-name-with-get
                                          short-name-with-post long-name long-name-with-method
                                          long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                               (set (keys client-metrics))
                               (set (keys client-metrics-data))))
                        (is (every? #(instance? Timer %) (vals client-metrics))))
               (testing "get-client-metrics-data returns a map of metric name to metric data"
                 (let [short-data (get client-metrics-data short-name)
                       short-data-get (get client-metrics-data short-name-with-get)
                       short-data-post (get client-metrics-data short-name-with-post)
                       long-data (get client-metrics-data long-name)]
                   (is (every? #(instance? ClientMetricData %) (vals client-metrics-data)))

                   (is (= short-name (.getMetricName short-data)))
                   (is (= 2 (.getCount short-data)))
                   (is (<= 5 (.getMean short-data)))
                   (is (<= 10 (.getAggregate short-data)))

                   (is (= short-name-with-get (.getMetricName short-data-get)))
                   (is (= 1 (.getCount short-data-get)))
                   (is (<= 5 (.getMean short-data-get)))
                   (is (<= 5 (.getAggregate short-data-get)))

                   (is (= short-name-with-post (.getMetricName short-data-post)))
                   (is (= 1 (.getCount short-data-post)))
                   (is (<= 5 (.getMean short-data-post)))
                   (is (<= 5 (.getAggregate short-data-post)))

                   (is (>= 1 (Math/abs (- (.getAggregate short-data)
                                          (+ (.getAggregate short-data-get)
                                             (.getAggregate short-data-post))))))

                   (is (= long-name (.getMetricName long-data)))
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
               (is (= nil (.getClientMetricsData client)))))))))))

(deftest metrics-test-clojure-async
  (testing "metrics work with clojure async client"
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (let [metric-registry (MetricRegistry.)]
        (with-open [client (async/create-client {:metric-registry metric-registry})]
          @(common/get client hello-url) ; warm it up
          (let [short-response @(common/get client short-url {:as :text :metric-id ["foo" "bar" "baz"]})
                long-response @(common/get client long-url)]
            @(common/post client short-url)
            (is (= {:status 200 :body "short"} (select-keys short-response [:status :body])))
            (is (= 200 (:status long-response)))
            (is (= "long" (slurp (:body long-response))))
            (.timer metric-registry "fake")
            (let [client-metrics (common/get-client-metrics client)
                  client-metrics-data (common/get-client-metrics-data client)
                  all-metrics (.getMetrics metric-registry)]
              (testing "get-client-metrics and get-client-metrics data return only http client metrics"
                (is (= 11 (count all-metrics)))
                (is (= 10 (count client-metrics)))
                (is (= 10 (count client-metrics-data))))
              (testing "get-client-metrics returns a map of metric name to timer instance"
                (is (= (set (list hello-name hello-name-with-method short-name short-name-with-get
                                  short-name-with-post long-name long-name-with-method
                                  long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                       (set (keys client-metrics))
                       (set (keys client-metrics-data))))
                (is (every? #(instance? Timer %) (vals client-metrics))))
              (testing "get-client-metrics-data returns a map of metric name to metrics data"
                (let [short-data (get client-metrics-data short-name)
                      short-data-get (get client-metrics-data short-name-with-get)
                      short-data-post (get client-metrics-data short-name-with-post)
                      long-data (get client-metrics-data long-name)]
                  (is (= short-name (:metric-name short-data)))
                  (is (= 2 (:count short-data)))
                  (is (<= 5 (:mean short-data)))
                  (is (<= 10 (:aggregate short-data)))

                  (is (= short-name-with-get (:metric-name short-data-get)))
                  (is (= 1 (:count short-data-get)))
                  (is (<= 5 (:mean short-data-get)))
                  (is (<= 5 (:aggregate short-data-get)))

                  (is (= short-name-with-post (:metric-name short-data-post)))
                  (is (= 1 (:count short-data-post)))
                  (is (<= 5 (:mean short-data-post)))
                  (is (<= 5 (:aggregate short-data-post)))

                  (is (>= 1 (Math/abs (- (:aggregate short-data)
                                         (+ (:aggregate short-data-get)
                                            (:aggregate short-data-post))))))

                  (is (= long-name (:metric-name long-data)))
                  (is (= 1 (:count long-data)))
                  (is (<= 100 (:mean long-data)))
                  (is (<= 100 (:aggregate long-data)))

                  (is (> (:mean long-data) (:mean short-data)))))))))
      (with-open [client (async/create-client {})]
        (testing "get-client-metrics returns nil if no metrics registry passed in"
          (let [response (common/get client hello-url)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))
            (is (= nil (common/get-client-metrics client)))
            (is (= nil (common/get-client-metrics-data client))))))))))

(deftest metrics-test-java-sync
  (testing "metrics work with java sync client"
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (let [metric-registry (MetricRegistry.)
            hello-request-opts (RequestOptions. hello-url)
            short-request-opts (RequestOptions. short-url)
            long-request-opts (doto (RequestOptions. long-url)
                                (.setMetricId (into-array ["foo" "bar" "baz"])))]
        (with-open [client (Sync/createClient (doto (ClientOptions.)
                                                (.setMetricRegistry metric-registry)))]
          (.get client hello-request-opts) ; warm it up
          (let [short-response (.get client short-request-opts)
                long-response (.get client long-request-opts)]
            (.post client short-request-opts)
            (is (= 200 (.getStatus short-response)))
            (is (= "short" (slurp (.getBody short-response))))
            (is (= 200 (.getStatus long-response)))
            (is (= "long" (slurp (.getBody long-response))))
            (.timer metric-registry "fake")
            (let [client-metrics (.getClientMetrics client)
                  client-metrics-data (.getClientMetricsData client)
                  all-metrics (.getMetrics metric-registry)]
              (testing ".getClientMetrics returns only http client metrics"
                (is (= 11 (count all-metrics)))
                (is (= 10 (count client-metrics)))
                (is (= 10 (count client-metrics-data))))
              (testing ".getClientMetrics returns a map of metric name to timer instance"
                (is (= (set (list hello-name hello-name-with-method short-name short-name-with-get
                                  short-name-with-post long-name long-name-with-method
                                  long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                       (set (keys client-metrics))
                       (set (keys client-metrics-data))))
                (is (every? #(instance? Timer %) (vals client-metrics))))
              (testing ".getClientMetricsData returns a map of metric name to metric data"
                (let [short-data (get client-metrics-data short-name)
                      short-data-get (get client-metrics-data short-name-with-get)
                      short-data-post (get client-metrics-data short-name-with-post)
                      long-data (get client-metrics-data long-name)]
                  (is (every? #(instance? ClientMetricData %) (vals client-metrics-data)))

                  (is (= short-name (.getMetricName short-data)))
                  (is (= 2 (.getCount short-data)))
                  (is (<= 5 (.getMean short-data)))
                  (is (<= 10 (.getAggregate short-data)))

                  (is (= short-name-with-get (.getMetricName short-data-get)))
                  (is (= 1 (.getCount short-data-get)))
                  (is (<= 5 (.getMean short-data-get)))
                  (is (<= 5 (.getAggregate short-data-get)))

                  (is (= short-name-with-post (.getMetricName short-data-post)))
                  (is (= 1 (.getCount short-data-post)))
                  (is (<= 5 (.getMean short-data-post)))
                  (is (<= 5 (.getAggregate short-data-post)))

                  (is (>= 1 (Math/abs (- (.getAggregate short-data)
                                         (+ (.getAggregate short-data-get)
                                            (.getAggregate short-data-post))))))

                  (is (= long-name (.getMetricName long-data)))
                  (is (= 1 (.getCount long-data)))
                  (is (<= 100 (.getMean long-data)))
                  (is (<= 100 (.getAggregate long-data)))

                  (is (> (.getMean long-data) (.getMean short-data))))))))
        (with-open [client (Sync/createClient (ClientOptions.))]
          (testing ".getClientMetrics returns nil if no metrics registry passed in"
            (let [response (.get client hello-request-opts)]
              (is (= 200 (.getStatus response)))
              (is (= "Hello, World!" (slurp (.getBody response))))
              (is (= nil (.getClientMetrics client)))
              (is (= nil (.getClientMetricsData client)))))))))))

(deftest metrics-test-clojure-sync
  (testing "metrics work with clojure sync client"
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (let [metric-registry (MetricRegistry.)]
        (with-open [client (sync/create-client {:metric-registry metric-registry})]
          (common/get client hello-url) ; warm it up
          (let [short-response (common/get client short-url {:as :text})
                long-response (common/get client long-url {:as :text :metric-id ["foo" "bar" "baz"]})]
            (common/post client short-url)
            (is (= {:status 200 :body "short"} (select-keys short-response [:status :body])))
            (is (= {:status 200 :body "long"} (select-keys long-response [:status :body])))
            (.timer metric-registry "fake")
            (let [client-metrics (common/get-client-metrics client)
                  client-metrics-data (common/get-client-metrics-data client)
                  all-metrics (.getMetrics metric-registry)]
              (testing "get-client-metrics and get-client-metrics data return only http client metrics"
                (is (= 11 (count all-metrics)))
                (is (= 10 (count client-metrics)))
                (is (= 10 (count client-metrics-data))))
              (testing "get-client-metrics returns a map of metric name to timer instance"
                (is (= (set (list hello-name hello-name-with-method short-name short-name-with-get
                                  short-name-with-post long-name long-name-with-method
                                  long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                       (set (keys client-metrics))
                       (set (keys client-metrics-data))))
                (is (every? #(instance? Timer %) (vals client-metrics))))
              (testing "get-client-metrics-data returns a map of metric name to metrics data"
                (let [short-data (get client-metrics-data short-name)
                      short-data-get (get client-metrics-data short-name-with-get)
                      short-data-post (get client-metrics-data short-name-with-post)
                      long-data (get client-metrics-data long-name)]
                  (is (= short-name (:metric-name short-data)))
                  (is (= 2 (:count short-data)))
                  (is (<= 5 (:mean short-data)))
                  (is (<= 10 (:aggregate short-data)))

                  (is (= short-name-with-get (:metric-name short-data-get)))
                  (is (= 1 (:count short-data-get)))
                  (is (<= 5 (:mean short-data-get)))
                  (is (<= 5 (:aggregate short-data-get)))

                  (is (= short-name-with-post (:metric-name short-data-post)))
                  (is (= 1 (:count short-data-post)))
                  (is (<= 5 (:mean short-data-post)))
                  (is (<= 5 (:aggregate short-data-post)))

                  (is (>= 1 (Math/abs (- (:aggregate short-data)
                                         (+ (:aggregate short-data-get)
                                            (:aggregate short-data-post))))))

                  (is (= long-name (:metric-name long-data)))
                  (is (= 1 (:count long-data)))
                  (is (<= 100 (:mean long-data)))
                  (is (<= 100 (:aggregate long-data)))

                  (is (> (:mean long-data) (:mean short-data))))))))
        (with-open [client (sync/create-client {})]
          (testing "get-client-metrics returns nil if no metrics registry passed in"
            (let [response (common/get client hello-url)]
              (is (= 200 (:status response)))
              (is (= "Hello, World!" (slurp (:body response))))
              (is (= nil (common/get-client-metrics client)))
              (is (= nil (common/get-client-metrics-data client)))))))))))

(deftest java-metrics-for-unbuffered-streaming-test
  (testlogging/with-test-logging
   (let [data (unbuffered-test/generate-data (* 1024 1024))]
     (testing "metrics work for a successful request"
       (let [metric-registry (MetricRegistry.)]
         (testwebserver/with-test-webserver-and-config
          (unbuffered-test/successful-handler data nil) port {:shutdown-timeout-seconds 1}
          (with-open [client (-> (ClientOptions.)
                                 (.setSocketTimeoutMilliseconds 20000)
                                 (.setConnectTimeoutMilliseconds 100)
                                 (.setMetricRegistry metric-registry)
                                 (Async/createClient))]
            (let [request-options (doto (RequestOptions. (str "http://localhost:" port "/hello"))
                                    (.setAs ResponseBodyType/UNBUFFERED_STREAM))
                  response (-> client (.get request-options) .deref)
                  status (.getStatus response)
                  body (.getBody response)]
              (is (= 200 status))
              (let [instream body
                    buf (make-array Byte/TYPE 4)]
                (.read instream buf)
                (is (= "xxxx" (String. buf "UTF-8"))) ;; Make sure we can read a few chars off of the stream
                (Thread/sleep 1000) ;; check that the bytes-read metric takes this into account
                (is (= (str data "yyyy") (str "xxxx" (slurp instream))))) ;; Read the rest and validate
              (let [client-metrics (.getClientMetrics client)
                    client-metrics-data (.getClientMetricsData client)
                    base-metric-name (str "puppetlabs.http-client.experimental.with-url.http://localhost:" port "/hello")
                    bytes-read-name (str base-metric-name ".bytes-read")
                    bytes-read-name-with-method (str base-metric-name ".GET" ".bytes-read")]
                (is (= (set (list bytes-read-name bytes-read-name-with-method))
                       (set (keys client-metrics))
                       (set (keys client-metrics-data))))
                (is (every? #(instance? Timer %) (vals client-metrics)))
                (let [bytes-read-data (get client-metrics-data bytes-read-name)]
                  (is (every? #(instance? ClientMetricData %) (vals client-metrics-data)))

                  (is (= 1 (.getCount bytes-read-data)))
                  (is (= bytes-read-name (.getMetricName bytes-read-data)))
                  (is (<= 1000 (.getMean bytes-read-data)))
                  (is (<= 1000 (.getAggregate bytes-read-data))))))))))
     (testing "metrics work for failed request"
       (try
         (testwebserver/with-test-webserver-and-config
          (unbuffered-test/blocking-handler data) port {:shutdown-timeout-seconds 1}
          (let [metric-registry (MetricRegistry.)]
            (with-open [client (-> (ClientOptions.)
                                   (.setSocketTimeoutMilliseconds 200)
                                   (.setConnectTimeoutMilliseconds 100)
                                   (.setMetricRegistry metric-registry)
                                   (Async/createClient))]
              (let [request-options (doto (RequestOptions. (str "http://localhost:" port "/hello"))
                                      (.setAs ResponseBodyType/UNBUFFERED_STREAM))
                    response (-> client (.get request-options) .deref)
                    error (.getError response)
                    body (.getBody response)]
                (is (nil? error))
                (is (thrown? SocketTimeoutException (slurp body)))
                (let [client-metrics (.getClientMetrics client)
                      client-metrics-data (.getClientMetricsData client)
                      base-metric-name (str "puppetlabs.http-client.experimental.with-url.http://localhost:" port "/hello")
                      bytes-read-name (str base-metric-name ".bytes-read")
                      bytes-read-name-with-method (str base-metric-name ".GET" ".bytes-read")]
                  (is (= (set (list bytes-read-name bytes-read-name-with-method))
                         (set (keys client-metrics))
                         (set (keys client-metrics-data))))
                  (is (every? #(instance? Timer %) (vals client-metrics)))
                  (let [bytes-read-data (get client-metrics-data bytes-read-name)]
                    (is (every? #(instance? ClientMetricData %) (vals client-metrics-data)))

                    (is (= 1 (.getCount bytes-read-data)))
                    (is (= bytes-read-name (.getMetricName bytes-read-data)))
                    (is (<= 200 (.getMean bytes-read-data)))
                    (is (<= 200 (.getAggregate bytes-read-data)))))))))
         (catch TimeoutException e
           ;; Expected whenever a server-side failure is generated
           ))))))

(deftest clojure-metrics-for-unbuffered-streaming-test
  (testlogging/with-test-logging
   (let [data (unbuffered-test/generate-data (* 1024 1024))
         opts {:as :unbuffered-stream}]
     (testing "metrics work for a successful request"
       (let [metric-registry (MetricRegistry.)]
         (testwebserver/with-test-webserver-and-config
          (unbuffered-test/successful-handler data nil) port {:shutdown-timeout-seconds 1}
          (with-open [client (async/create-client {:connect-timeout-milliseconds 100
                                                   :socket-timeout-milliseconds 20000
                                                   :metric-registry metric-registry})]
            (let [response @(common/get client (str "http://localhost:" port "/hello") opts)
                  {:keys [status body]} response]
              (is (= 200 status))
              (let [instream body
                    buf (make-array Byte/TYPE 4)]
                (.read instream buf)
                (is (= "xxxx" (String. buf "UTF-8"))) ;; Make sure we can read a few chars off of the stream
                (Thread/sleep 1000) ;; check that the bytes-read metric takes this into account
                (is (= (str data "yyyy") (str "xxxx" (slurp instream))))) ;; Read the rest and validate
              (let [client-metrics (common/get-client-metrics client)
                    client-metrics-data (common/get-client-metrics-data client)
                    base-metric-name (str "puppetlabs.http-client.experimental.with-url.http://localhost:" port "/hello")
                    bytes-read-name (str base-metric-name ".bytes-read")
                    bytes-read-name-with-method (str base-metric-name ".GET" ".bytes-read")]
                (is (= (set (list bytes-read-name bytes-read-name-with-method))
                       (set (keys client-metrics))
                       (set (keys client-metrics-data))))
                (is (every? #(instance? Timer %) (vals client-metrics)))
                (let [bytes-read-data (get client-metrics-data bytes-read-name)]
                  (is (= {:count 1 :metric-name bytes-read-name}
                         (select-keys bytes-read-data [:metric-name :count])))
                  (is (<= 1000 (:mean bytes-read-data)))
                  (is (<= 1000 (:aggregate bytes-read-data))))))))))
     (testing "metrics work for a failed request"
       (try
         (testwebserver/with-test-webserver-and-config
          (unbuffered-test/blocking-handler data) port {:shutdown-timeout-seconds 1}
          (let [metric-registry (MetricRegistry.)]
            (with-open [client (async/create-client {:connect-timeout-milliseconds 100
                                                     :socket-timeout-milliseconds 200
                                                     :metric-registry metric-registry})]
              (let [response @(common/get client (str "http://localhost:" port "/hello") opts)
                    {:keys [body error]} response]
                (is (nil? error))
                ;; Consume the body to get the exception
                (is (thrown? SocketTimeoutException (slurp body))))
              (let [client-metrics (common/get-client-metrics client)
                    client-metrics-data (common/get-client-metrics-data client)
                    base-metric-name (str "puppetlabs.http-client.experimental.with-url.http://localhost:" port "/hello")
                    bytes-read-name (str base-metric-name ".bytes-read")
                    bytes-read-name-with-method (str base-metric-name ".GET" ".bytes-read")]
                (is (= (set (list bytes-read-name bytes-read-name-with-method))
                       (set (keys client-metrics))
                       (set (keys client-metrics-data))))
                (is (every? #(instance? Timer %) (vals client-metrics)))
                (let [bytes-read-data (get client-metrics-data bytes-read-name)]
                  (is (= {:count 1 :metric-name bytes-read-name}
                         (select-keys bytes-read-data [:metric-name :count])))
                  (is (<= 200 (:mean bytes-read-data)))
                  (is (<= 200 (:aggregate bytes-read-data))))))))
         (catch TimeoutException e
           ;; Expected whenever a server-side failure is generated
           ))))))
