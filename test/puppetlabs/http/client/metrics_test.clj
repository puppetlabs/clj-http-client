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
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.http.client.metrics :as metrics]
            [schema.test :as schema-test])
  (:import (com.puppetlabs.http.client Async RequestOptions
                                       ClientOptions ResponseBodyType Sync)
           (com.codahale.metrics MetricRegistry)
           (java.net SocketTimeoutException)
           (java.util.concurrent TimeoutException)
           (com.puppetlabs.http.client.metrics Metrics ClientTimer ClientMetricData)))

(use-fixtures :once schema-test/validate-schemas)

(def metric-namespace "puppetlabs.http-client.experimental")

(tk/defservice test-metric-web-service
               [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler
         (fn [_] {:status 200 :body "Hello, World!"}) "/hello")
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

(def short-name (format "%s.with-url.%s.full-response" metric-namespace short-url))
(def short-name-with-get (format "%s.with-url-and-method.%s.GET.full-response"
                                 metric-namespace short-url))
(def short-name-with-post (format "%s.with-url-and-method.%s.POST.full-response"
                                  metric-namespace short-url))

(def long-name (format "%s.with-url.%s.full-response" metric-namespace long-url))
(def long-name-with-method (format "%s.with-url-and-method.%s.GET.full-response"
                                   metric-namespace long-url))

(def long-foo-name
  "puppetlabs.http-client.experimental.with-metric-id.foo.full-response")
(def long-foo-bar-name
  "puppetlabs.http-client.experimental.with-metric-id.foo.bar.full-response")
(def long-foo-bar-baz-name
  "puppetlabs.http-client.experimental.with-metric-id.foo.bar.baz.full-response")

(def hello-name (format "%s.with-url.%s.full-response" metric-namespace hello-url))
(def hello-name-with-method (format "%s.with-url-and-method.%s.GET.full-response"
                                    metric-namespace hello-url))

(deftest metrics-test-java-async
  (testing "metrics work with java async client"
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (with-open [client (Async/createClient (doto (ClientOptions.)
                                               (.setMetricRegistry (MetricRegistry.))
                                               (.setEnableURLMetrics false)))]
        (let [request-opts (RequestOptions. hello-url)
              response (-> client (.get request-opts) (.deref))]
          (is (= 200 (.getStatus response)))
          (let [client-metrics (-> client
                                   (.getMetricRegistry)
                                   (Metrics/getClientMetrics))]
            (is (.isEmpty (.getUrlTimers client-metrics)))
            (is (.isEmpty (.getUrlAndMethodTimers client-metrics))))))
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
            (let [client-metric-registry (.getMetricRegistry client)
                  client-metrics (Metrics/getClientMetrics client-metric-registry)
                  client-metrics-data (Metrics/getClientMetricsData client-metric-registry)
                  url-metrics (.getUrlTimers client-metrics)
                  url-and-method-metrics (.getUrlAndMethodTimers client-metrics)
                  metric-id-metrics (.getMetricIdTimers client-metrics)
                  url-metrics-data (.getUrlData client-metrics-data)
                  url-and-method-metrics-data (.getUrlAndMethodData client-metrics-data)
                  metric-id-metrics-data (.getMetricIdData client-metrics-data)
                  all-metrics (.getMetrics metric-registry)]
              (testing ".getMetricRegistry returns the associated MetricRegistry"
                (is (instance? MetricRegistry client-metric-registry)))
              (testing "Metrics/getClientMetrics returns only http client metrics"
                (is (= 11 (count all-metrics)))
                (is (= 10 (+ (count url-metrics)
                             (count url-and-method-metrics)
                             (count metric-id-metrics))))
                (is (= 10 (+ (count url-metrics-data)
                             (count url-and-method-metrics-data)
                             (count metric-id-metrics-data)))))
              (testing ".getClientMetrics returns a map of category to array of timers"
                (is (= (set (list hello-name short-name long-name))
                       (set (map #(.getMetricName %) url-metrics))
                       (set (map #(.getMetricName %) url-metrics-data))))
                (is (= (set (list hello-name-with-method short-name-with-get
                                  short-name-with-post long-name-with-method))
                       (set (map #(.getMetricName %) url-and-method-metrics))
                       (set (map #(.getMetricName %) url-and-method-metrics-data))))
                (is (= (set (list long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                       (set (map #(.getMetricName %) metric-id-metrics))
                       (set (map #(.getMetricName %) metric-id-metrics-data))))
                (is (every? #(instance? ClientTimer %) url-metrics))
                (is (every? #(instance? ClientTimer %) url-and-method-metrics))
                (is (every? #(instance? ClientTimer %) metric-id-metrics)))
              (testing ".getClientMetricsData returns a map of metric category to arrays of metric data"
                (let [short-data (first (filter #(= short-name (.getMetricName %)) url-metrics-data))
                      short-data-get (first (filter #(= short-name-with-get (.getMetricName %))
                                                    url-and-method-metrics-data))
                      short-data-post (first (filter #(= short-name-with-post (.getMetricName %))
                                                     url-and-method-metrics-data))
                      long-data (first (filter #(= long-name (.getMetricName %)) url-metrics-data))]
                  (is (every? #(instance? ClientMetricData %)
                              (concat url-metrics-data
                                      url-and-method-metrics-data
                                      metric-id-metrics-data)))

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
          (testing ".getMetricRegistry returns nil if no metric registry passed in"
            (is (= nil (.getMetricRegistry client))))))))))

(deftest metrics-test-clojure-async
  (testing "metrics work with clojure async client"
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (with-open [client (async/create-client {:metric-registry (MetricRegistry.)
                                               :enable-url-metrics? false})]
        (let [response @(common/get client hello-url)]
          (is (= 200 (:status response)))
          (let [client-metrics (-> client
                                   (common/get-client-metric-registry)
                                   (metrics/get-client-metrics))]
            (is (empty? (:url client-metrics)))
            (is (empty? (:url-and-method client-metrics))))))
      (let [metric-registry (MetricRegistry.)]
        (with-open [client (async/create-client
                            {:metric-registry metric-registry})]
          @(common/get client hello-url) ; warm it up
          (let [short-response @(common/get client short-url {:as :text :metric-id ["foo" "bar" "baz"]})
                long-response @(common/get client long-url)]
            @(common/post client short-url)
            (is (= {:status 200 :body "short"} (select-keys short-response [:status :body])))
            (is (= 200 (:status long-response)))
            (is (= "long" (slurp (:body long-response))))
            (.timer metric-registry "fake")
            (let [client-metric-registry (common/get-client-metric-registry client)
                  client-metrics (metrics/get-client-metrics client-metric-registry)
                  client-metrics-data (metrics/get-client-metrics-data client-metric-registry)
                  url-metrics (:url client-metrics)
                  url-and-method-metrics (:url-and-method client-metrics)
                  metric-id-metrics (:metric-id client-metrics)
                  url-metrics-data (:url client-metrics-data)
                  url-and-method-metrics-data (:url-and-method client-metrics-data)
                  metric-id-metrics-data (:metric-id client-metrics-data)
                  all-metrics (.getMetrics metric-registry)]
              (testing "get-client-metric-registry returns the associated MetricRegistry"
                (is (instance? MetricRegistry client-metric-registry)))
              (testing "get-client-metrics and get-client-metrics data return only http client metrics"
                (is (= 11 (count all-metrics)))
                (is (= 10 (+ (count url-metrics)
                             (count url-and-method-metrics)
                             (count metric-id-metrics))))
                (is (= 10 (+ (count url-metrics-data)
                             (count url-and-method-metrics-data)
                             (count metric-id-metrics-data)))))
              (testing "get-client-metrics returns a map of category to array of timers"
                (is (= (set (list hello-name short-name long-name))
                       (set (map #(.getMetricName %) url-metrics))
                       (set (map :metric-name url-metrics-data))))
                (is (= (set (list hello-name-with-method short-name-with-get
                                  short-name-with-post long-name-with-method))
                       (set (map #(.getMetricName %) url-and-method-metrics))
                       (set (map :metric-name url-and-method-metrics-data))))
                (is (= (set (list long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                       (set (map #(.getMetricName %) metric-id-metrics))
                       (set (map :metric-name metric-id-metrics-data))))
                (is (every? #(instance? ClientTimer %) url-metrics))
                (is (every? #(instance? ClientTimer %) url-and-method-metrics))
                (is (every? #(instance? ClientTimer %) metric-id-metrics)))
              (testing "get-client-metrics-data returns a map of metric category to metric data"
                (let [short-data (first (filter #(= short-name (:metric-name %)) url-metrics-data))
                      short-data-get (first (filter #(= short-name-with-get (:metric-name %))
                                                    url-and-method-metrics-data))
                      short-data-post (first (filter #(= short-name-with-post (:metric-name %))
                                                     url-and-method-metrics-data))
                      long-data (first (filter #(= long-name (:metric-name %)) url-metrics-data))]
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
        (testing "get-client-metric-registry returns nil if no metric registry passed in"
          (is (= nil (common/get-client-metric-registry client)))))))))

(deftest metrics-test-java-sync
  (testing "metrics work with java sync client"
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (with-open [client (Sync/createClient (doto (ClientOptions.)
                                              (.setMetricRegistry (MetricRegistry.))
                                              (.setEnableURLMetrics false)))]
        (let [request-opts (RequestOptions. hello-url)
              response (-> client (.get request-opts))]
          (is (= 200 (.getStatus response)))
          (let [client-metrics (-> client
                                   (.getMetricRegistry)
                                   (Metrics/getClientMetrics))]
            (is (.isEmpty (.getUrlTimers client-metrics)))
            (is (.isEmpty (.getUrlAndMethodTimers client-metrics))))))
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
            (let [client-metric-registry (.getMetricRegistry client)
                  client-metrics (Metrics/getClientMetrics client-metric-registry)
                  client-metrics-data (Metrics/getClientMetricsData client-metric-registry)
                  url-metrics (.getUrlTimers client-metrics)
                  url-and-method-metrics (.getUrlAndMethodTimers client-metrics)
                  metric-id-metrics (.getMetricIdTimers client-metrics)
                  url-metrics-data (.getUrlData client-metrics-data)
                  url-and-method-metrics-data (.getUrlAndMethodData client-metrics-data)
                  metric-id-metrics-data (.getMetricIdData client-metrics-data)
                  all-metrics (.getMetrics metric-registry)]
              (testing ".getMetricRegistry returns the associated MetricRegistry"
                (is (instance? MetricRegistry client-metric-registry)))
              (testing "Metrics/getClientMetrics returns only http client metrics"
                (is (= 11 (count all-metrics)))
                (is (= 10 (+ (count url-metrics)
                             (count url-and-method-metrics)
                             (count metric-id-metrics))))
                (is (= 10 (+ (count url-metrics-data)
                             (count url-and-method-metrics-data)
                             (count metric-id-metrics-data)))))
              (testing ".getClientMetrics returns a map of category to array of timers"

                (is (= (set (list hello-name short-name long-name))
                       (set (map #(.getMetricName %) url-metrics))
                       (set (map #(.getMetricName %) url-metrics-data))))
                (is (= (set (list hello-name-with-method short-name-with-get
                                  short-name-with-post long-name-with-method))
                       (set (map #(.getMetricName %) url-and-method-metrics))
                       (set (map #(.getMetricName %) url-and-method-metrics-data))))
                (is (= (set (list long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                       (set (map #(.getMetricName %) metric-id-metrics))
                       (set (map #(.getMetricName %) metric-id-metrics-data))))
                (is (every? #(instance? ClientTimer %) url-metrics))
                (is (every? #(instance? ClientTimer %) url-and-method-metrics))
                (is (every? #(instance? ClientTimer %) metric-id-metrics)))
              (testing ".getClientMetricsData returns a map of metric category to arrays of metric data"
                (let [short-data (first (filter #(= short-name (.getMetricName %)) url-metrics-data))
                      short-data-get (first (filter #(= short-name-with-get (.getMetricName %))
                                                    url-and-method-metrics-data))
                      short-data-post (first (filter #(= short-name-with-post (.getMetricName %))
                                                     url-and-method-metrics-data))
                      long-data (first (filter #(= long-name (.getMetricName %)) url-metrics-data))]
                  (is (every? #(instance? ClientMetricData %)
                              (concat url-metrics-data
                                      url-and-method-metrics-data
                                      metric-id-metrics-data)))

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
          (testing ".getMetricRegistry returns nil if no metric registry passed in"
            (is (= nil (.getMetricRegistry client))))))))))

(deftest metrics-test-clojure-sync
  (testing "metrics work with clojure sync client"
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (with-open [client (sync/create-client {:metric-registry (MetricRegistry.)
                                              :enable-url-metrics? false})]
        (let [response (common/get client hello-url)]
          (is (= 200 (:status response)))
          (let [client-metrics (-> client
                                   (common/get-client-metric-registry)
                                   (metrics/get-client-metrics))]
            (is (empty? (:url client-metrics)))
            (is (empty? (:url-and-method client-metrics))))))
      (let [metric-registry (MetricRegistry.)]
        (with-open [client (sync/create-client {:metric-registry metric-registry})]
          (common/get client hello-url) ; warm it up
          (let [short-response (common/get client short-url {:as :text})
                long-response (common/get client long-url {:as :text :metric-id ["foo" "bar" "baz"]})]
            (common/post client short-url)
            (is (= {:status 200 :body "short"} (select-keys short-response [:status :body])))
            (is (= {:status 200 :body "long"} (select-keys long-response [:status :body])))
            (.timer metric-registry "fake")
            (let [client-metric-registry (common/get-client-metric-registry client)
                  client-metrics (metrics/get-client-metrics client-metric-registry)
                  client-metrics-data (metrics/get-client-metrics-data client-metric-registry)
                  url-metrics (:url client-metrics)
                  url-and-method-metrics (:url-and-method client-metrics)
                  metric-id-metrics (:metric-id client-metrics)
                  url-metrics-data (:url client-metrics-data)
                  url-and-method-metrics-data (:url-and-method client-metrics-data)
                  metric-id-metrics-data (:metric-id client-metrics-data)
                  all-metrics (.getMetrics metric-registry)]
              (testing "get-client-metric-registry returns the associated MetricRegistry"
                (is (instance? MetricRegistry client-metric-registry)))
              (testing "get-client-metrics and get-client-metrics data return only http client metrics"
                (is (= 11 (count all-metrics)))
                (is (= 10 (+ (count url-metrics)
                             (count url-and-method-metrics)
                             (count metric-id-metrics))))
                (is (= 10 (+ (count url-metrics-data)
                             (count url-and-method-metrics-data)
                             (count metric-id-metrics-data)))))
              (testing "get-client-metrics returns a map of category to array of timers"
                (is (= (set (list hello-name short-name long-name))
                       (set (map #(.getMetricName %) url-metrics))
                       (set (map :metric-name url-metrics-data))))
                (is (= (set (list hello-name-with-method short-name-with-get
                                  short-name-with-post long-name-with-method))
                       (set (map #(.getMetricName %) url-and-method-metrics))
                       (set (map :metric-name url-and-method-metrics-data))))
                (is (= (set (list long-foo-name long-foo-bar-name long-foo-bar-baz-name))
                       (set (map #(.getMetricName %) metric-id-metrics))
                       (set (map :metric-name metric-id-metrics-data))))
                (is (every? #(instance? ClientTimer %) url-metrics))
                (is (every? #(instance? ClientTimer %) url-and-method-metrics))
                (is (every? #(instance? ClientTimer %) metric-id-metrics)))
              (testing "get-client-metrics-data returns a map of metric category to metric data"
                (let [short-data (first (filter #(= short-name (:metric-name %)) url-metrics-data))
                      short-data-get (first (filter #(= short-name-with-get (:metric-name %))
                                                    url-and-method-metrics-data))
                      short-data-post (first (filter #(= short-name-with-post (:metric-name %))
                                                     url-and-method-metrics-data))
                      long-data (first (filter #(= long-name (:metric-name %)) url-metrics-data))]
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
          (testing "get-client-metric-registry returns nil if no metric registry passed in"
            (is (= nil (common/get-client-metric-registry client))))))))))

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
            (let [url (str "http://localhost:" port "/hello")
                  request-options (doto (RequestOptions. url)
                                    (.setAs ResponseBodyType/UNBUFFERED_STREAM))
                  response (-> client (.get request-options) .deref)
                  status (.getStatus response)
                  body (.getBody response)]
              (is (= 200 status))
              (let [instream body
                    buf (make-array Byte/TYPE 4)]
                (.read instream buf)
                (is (= "xxxx" (String. buf "UTF-8"))) ;; Make sure we can read a few chars off of the stream
                (Thread/sleep 1000) ;; check that the full-response metric takes this into account
                (is (= (str data "yyyy") (str "xxxx" (slurp instream))))) ;; Read the rest and validate
              (let [client-metric-registry (.getMetricRegistry client)
                    client-metrics (Metrics/getClientMetrics client-metric-registry)
                    client-metrics-data (Metrics/getClientMetricsData client-metric-registry)
                    full-response-name (format "%s.with-url.%s.full-response" metric-namespace url)
                    full-response-name-with-method (format "%s.with-url-and-method.%s.GET.full-response"
                                                        metric-namespace url)]
                (is (= [full-response-name]
                       (map #(.getMetricName %) (.getUrlTimers client-metrics))
                       (map #(.getMetricName %) (.getUrlData client-metrics-data))))
                (is (= [full-response-name-with-method]
                       (map #(.getMetricName %) (.getUrlAndMethodTimers client-metrics))
                       (map #(.getMetricName %) (.getUrlAndMethodData client-metrics-data))))
                (is (= [] (.getMetricIdTimers client-metrics) (.getMetricIdData client-metrics-data)))
                (is (every? #(instance? ClientTimer %)
                            (concat (.getUrlTimers client-metrics)
                                    (.getUrlAndMethodTimers client-metrics))))
                (let [full-response-data (first (.getUrlData client-metrics-data))]
                  (is (every? #(instance? ClientMetricData %)
                              (concat (.getUrlData client-metrics-data)
                                      (.getUrlAndMethodData client-metrics-data))))

                  (is (= 1 (.getCount full-response-data)))
                  (is (= full-response-name (.getMetricName full-response-data)))
                  (is (<= 1000 (.getMean full-response-data)))
                  (is (<= 1000 (.getAggregate full-response-data))))))))))
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
              (let [url (str "http://localhost:" port "/hello")
                    request-options (doto (RequestOptions. url)
                                      (.setAs ResponseBodyType/UNBUFFERED_STREAM))
                    response (-> client (.get request-options) .deref)
                    error (.getError response)
                    body (.getBody response)]
                (is (nil? error))
                (is (thrown? SocketTimeoutException (slurp body)))
                (let [client-metric-registry (.getMetricRegistry client)
                      client-metrics (Metrics/getClientMetrics client-metric-registry)
                      client-metrics-data (Metrics/getClientMetricsData client-metric-registry)
                      full-response-name (format "%s.with-url.%s.full-response" metric-namespace url)
                      full-response-name-with-method (format "%s.with-url-and-method.%s.GET.full-response"
                                                          metric-namespace url)]
                  (is (= [full-response-name]
                         (map #(.getMetricName %) (.getUrlTimers client-metrics))
                         (map #(.getMetricName %) (.getUrlData client-metrics-data))))
                  (is (= [full-response-name-with-method]
                         (map #(.getMetricName %) (.getUrlAndMethodTimers client-metrics))
                         (map #(.getMetricName %) (.getUrlAndMethodData client-metrics-data))))
                  (is (= [] (.getMetricIdTimers client-metrics)
                         (.getMetricIdData client-metrics-data)))
                  (is (every? #(instance? ClientTimer %)
                              (concat (.getUrlTimers client-metrics)
                                      (.getUrlAndMethodTimers client-metrics))))
                  (let [full-response-data (first (.getUrlData client-metrics-data))]
                    (is (every? #(instance? ClientMetricData %)
                                (concat (.getUrlData client-metrics-data)
                                        (.getUrlAndMethodData client-metrics-data))))

                    (is (= 1 (.getCount full-response-data)))
                    (is (= full-response-name (.getMetricName full-response-data)))
                    (is (<= 200 (.getMean full-response-data)))
                    (is (<= 200 (.getAggregate full-response-data)))))))))
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
            (let [url (str "http://localhost:" port "/hello")
                  response @(common/get client url opts)
                  {:keys [status body]} response]
              (is (= 200 status))
              (let [instream body
                    buf (make-array Byte/TYPE 4)]
                (.read instream buf)
                (is (= "xxxx" (String. buf "UTF-8"))) ;; Make sure we can read a few chars off of the stream
                (Thread/sleep 1000) ;; check that the full-response metric takes this into account
                (is (= (str data "yyyy") (str "xxxx" (slurp instream))))) ;; Read the rest and validate
              (let [client-metric-registry (common/get-client-metric-registry client)
                    client-metrics (metrics/get-client-metrics client-metric-registry)
                    client-metrics-data (metrics/get-client-metrics-data client-metric-registry)
                    full-response-name (format "%s.with-url.%s.full-response" metric-namespace url)
                    full-response-name-with-method (format "%s.with-url-and-method.%s.GET.full-response"
                                                        metric-namespace url)]
                (is (= [full-response-name]
                       (map #(.getMetricName %) (:url client-metrics))
                       (map :metric-name (:url client-metrics-data))))
                (is (= [full-response-name-with-method]
                       (map #(.getMetricName %) (:url-and-method client-metrics))
                       (map :metric-name  (:url-and-method client-metrics-data))))
                (is (= [] (:metric-id client-metrics) (:metric-id client-metrics-data)))
                (is (every? #(instance? ClientTimer %)
                            (concat (:url client-metrics)
                                    (:url-and-method client-metrics))))
                (let [full-response-data (first (:url client-metrics-data))]
                  (is (= {:count 1 :metric-name full-response-name}
                         (select-keys full-response-data [:metric-name :count])))
                  (is (<= 1000 (:mean full-response-data)))
                  (is (<= 1000 (:aggregate full-response-data))))))))))
     (testing "metrics work for a failed request"
       (try
         (testwebserver/with-test-webserver-and-config
          (unbuffered-test/blocking-handler data) port {:shutdown-timeout-seconds 1}
          (let [metric-registry (MetricRegistry.)
                url (str "http://localhost:" port "/hello")]
            (with-open [client (async/create-client {:connect-timeout-milliseconds 100
                                                     :socket-timeout-milliseconds 200
                                                     :metric-registry metric-registry})]
              (let [response @(common/get client url opts)
                    {:keys [body error]} response]
                (is (nil? error))
                ;; Consume the body to get the exception
                (is (thrown? SocketTimeoutException (slurp body))))
              (let [client-metric-registry (common/get-client-metric-registry client)
                    client-metrics (metrics/get-client-metrics client-metric-registry)
                    client-metrics-data (metrics/get-client-metrics-data client-metric-registry)
                    full-response-name (format "%s.with-url.%s.full-response" metric-namespace url)
                    full-response-name-with-method (format "%s.with-url-and-method.%s.GET.full-response"
                                                        metric-namespace url)]
                (is (= [full-response-name]
                       (map #(.getMetricName %) (:url client-metrics))
                       (map :metric-name (:url client-metrics-data))))
                (is (= [full-response-name-with-method]
                       (map #(.getMetricName %) (:url-and-method client-metrics))
                       (map :metric-name (:url-and-method client-metrics-data))))
                (is (= [] (:metric-id client-metrics) (:metric-id client-metrics-data)))
                (is (every? #(instance? ClientTimer %)
                            (concat (:url client-metrics)
                                    (:url-and-method client-metrics))))
                (let [full-response-data (first (:url client-metrics-data))]
                  (is (= {:count 1 :metric-name full-response-name}
                         (select-keys full-response-data [:metric-name :count])))
                  (is (<= 200 (:mean full-response-data)))
                  (is (<= 200 (:aggregate full-response-data))))))))
         (catch TimeoutException e
           ;; Expected whenever a server-side failure is generated
           ))))))

(deftest metric-namespace-test
  (let [metric-prefix "my-metric-prefix"
        server-id "my-server"
        metric-name-with-prefix
        (format "%s.http-client.experimental.with-url.%s.full-response" metric-prefix hello-url)
        metric-name-with-server-id
        (format "puppetlabs.%s.http-client.experimental.with-url.%s.full-response"
                server-id hello-url)
        get-metric-name (fn [metric-registry]
                          (.getMetricName (first (Metrics/getClientMetricsDataByUrl
                                                  metric-registry hello-url))))]
    (testlogging/with-test-logging
     (testutils/with-app-with-config
      app
      [jetty9/jetty9-service test-metric-web-service]
      {:webserver {:port 10000}}
      (testing "custom metric namespace works for java async client"
        (testing "metric prefix works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-metric-prefix (Async/createClient
                                                   (doto (ClientOptions.)
                                                     (.setMetricRegistry metric-registry)
                                                     (.setMetricPrefix metric-prefix)))]
              (is (= (format "%s.http-client.experimental" metric-prefix)
                     (.getMetricNamespace client-with-metric-prefix)))
              (-> client-with-metric-prefix (.get (RequestOptions. hello-url)))
              (is (= metric-name-with-prefix (get-metric-name metric-registry))))))
        (testing "server id works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (Async/createClient
                                               (doto (ClientOptions.)
                                                 (.setMetricRegistry metric-registry)
                                                 (.setServerId server-id)))]
              (-> client-with-server-id (.get (RequestOptions. hello-url)))
              (is (= metric-name-with-server-id (get-metric-name metric-registry))))))
        (testing "metric prefix overrides server id if both are set"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (Async/createClient
                                               (doto (ClientOptions.)
                                                 (.setMetricRegistry metric-registry)
                                                 (.setMetricPrefix metric-prefix)
                                                 (.setServerId server-id)))]
              (-> client-with-server-id (.get (RequestOptions. hello-url)))
              (is (= metric-name-with-prefix (get-metric-name metric-registry)))
              (is (logged? #"Metric prefix and server id both set.*" :warn))))))
      (testing "custom metric namespace works for clojure async client"
        (testing "metric prefix works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-metric-prefix (async/create-client
                                                   {:metric-registry metric-registry
                                                    :metric-prefix metric-prefix})]
              (is (= (format "%s.http-client.experimental" metric-prefix)
                     (common/get-client-metric-namespace client-with-metric-prefix)))
              (-> client-with-metric-prefix (common/get hello-url))
              (is (= metric-name-with-prefix (get-metric-name metric-registry))))))
        (testing "server id works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (async/create-client
                                                   {:metric-registry metric-registry
                                                    :server-id server-id})]
              (-> client-with-server-id (common/get hello-url))
              (is (= metric-name-with-server-id (get-metric-name metric-registry))))))
        (testing "metric prefix overrides server id if both are set"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (async/create-client
                                               {:metric-registry metric-registry
                                                :metric-prefix metric-prefix
                                                :server-id server-id})]
              (-> client-with-server-id (common/get hello-url))
              (is (= metric-name-with-prefix (get-metric-name metric-registry)))))))
      (testing "custom metric namespace works for Java sync client"
        (testing "metric prefix works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-metric-prefix (Sync/createClient
                                                   (doto (ClientOptions.)
                                                     (.setMetricRegistry metric-registry)
                                                     (.setMetricPrefix metric-prefix)))]
              (is (= (format "%s.http-client.experimental" metric-prefix)
                     (.getMetricNamespace client-with-metric-prefix)))
              (-> client-with-metric-prefix (.get (RequestOptions. hello-url)))
              (is (= metric-name-with-prefix (get-metric-name metric-registry))))))
        (testing "server id works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (Sync/createClient
                                               (doto (ClientOptions.)
                                                 (.setMetricRegistry metric-registry)
                                                 (.setServerId server-id)))]
              (-> client-with-server-id (.get (RequestOptions. hello-url)))
              (is (= metric-name-with-server-id (get-metric-name metric-registry))))))
        (testing "metric prefix overrides server id if both are set"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (Sync/createClient
                                               (doto (ClientOptions.)
                                                 (.setMetricRegistry metric-registry)
                                                 (.setMetricPrefix metric-prefix)
                                                 (.setServerId server-id)))]
              (-> client-with-server-id (.get (RequestOptions. hello-url)))
              (is (= metric-name-with-prefix (get-metric-name metric-registry)))))))
      (testing "custom metric namespace works for clojure sync client"
        (testing "metric prefix works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-metric-prefix (sync/create-client
                                                   {:metric-registry metric-registry
                                                    :metric-prefix metric-prefix})]
              (is (= (format "%s.http-client.experimental" metric-prefix)
                     (common/get-client-metric-namespace client-with-metric-prefix)))
              (-> client-with-metric-prefix (common/get hello-url))
              (is (= metric-name-with-prefix (get-metric-name metric-registry))))))
        (testing "server id works"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (sync/create-client
                                                   {:metric-registry metric-registry
                                                    :server-id server-id})]
              (-> client-with-server-id (common/get hello-url))
              (is (= metric-name-with-server-id (get-metric-name metric-registry))))))
        (testing "metric prefix overrides server id if both are set"
          (let [metric-registry (MetricRegistry.)]
            (with-open [client-with-server-id (sync/create-client
                                               {:metric-registry metric-registry
                                                :metric-prefix metric-prefix
                                                :server-id server-id})]
              (-> client-with-server-id (common/get hello-url))
              (is (= metric-name-with-prefix (get-metric-name metric-registry)))))))))))
