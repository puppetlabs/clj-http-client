(ns com.puppetlabs.http.client.impl.metrics-unit-test
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.metrics :as metrics])
  (:import (com.codahale.metrics MetricRegistry)
           (com.puppetlabs.http.client.impl Metrics Metrics$MetricType)
           (org.apache.http.message BasicHttpRequest)))

(def bytes-read Metrics$MetricType/BYTES_READ)

(defn add-metric-ns [string]
  (str "puppetlabs.http-client.experimental." string))

(deftest start-bytes-read-timers-test
  (testing "startBytesReadTimers creates the right timers"
    (let [url-id (add-metric-ns "with-url.http://localhost/foo.bytes-read")
          url-method-id (add-metric-ns "with-url-and-method.http://localhost/foo.GET.bytes-read")]
      (testing "metric id timers are not created for a request without a metric id"
        (let [metric-registry (MetricRegistry.)]
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://localhost/foo")
                                        nil)
          (is (= (set (list url-id url-method-id)) (set (keys (.getTimers metric-registry)))))))
      (testing "metric id timers are not created for a request with an empty metric id"
        (let [metric-registry (MetricRegistry.)]
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://localhost/foo")
                                        (into-array String []))
          (is (= (set (list url-id url-method-id)) (set (keys (.getTimers metric-registry)))))))
      (testing "metric id timers are created correctly for a request with a metric id"
        (let [metric-registry (MetricRegistry.)]
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://localhost/foo")
                                        (into-array ["foo" "bar" "baz"]))
          (is (= (set (list url-id url-method-id
                            (add-metric-ns "with-metric-id.foo.bytes-read")
                            (add-metric-ns "with-metric-id.foo.bar.bytes-read")
                            (add-metric-ns "with-metric-id.foo.bar.baz.bytes-read")))
                 (set (keys (.getTimers metric-registry)))))))
      (testing "url timers should strip off username, password, query string, and fragment"
        (let [metric-registry (MetricRegistry.)]
          (Metrics/startBytesReadTimers
           metric-registry
           (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?te%2cst=one")
           nil)
          (Metrics/startBytesReadTimers
           metric-registry
           (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz#x%2cyz")
           nil)
          (Metrics/startBytesReadTimers
           metric-registry
           (BasicHttpRequest.
            "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?te%2cst=one#x%2cyz")
           nil)
          (Metrics/startBytesReadTimers
           metric-registry
           (BasicHttpRequest.
            "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?#x%2cyz")
           nil)
          (is (= (set (list
                       (add-metric-ns
                        "with-url.http://localhost:1234/foo%2cbar/baz.bytes-read")
                       (add-metric-ns
                        "with-url-and-method.http://localhost:1234/foo%2cbar/baz.GET.bytes-read")))
                 (set (keys (.getTimers metric-registry))))))))))

(defn start-and-stop-timers! [registry req id]
  (doseq [timer (Metrics/startBytesReadTimers
                 registry
                 req
                 id)]
    (.stop timer)))

(deftest get-client-metrics-data-test
  (let [registry (MetricRegistry.)
        url "http://test.com/one"
        url2 "http://test.com/one/two"]
    (start-and-stop-timers! registry (BasicHttpRequest. "GET" url) nil)
    (start-and-stop-timers! registry (BasicHttpRequest. "POST" url) nil)
    (start-and-stop-timers! registry (BasicHttpRequest. "POST" url) (into-array ["foo" "bar"]))
    (start-and-stop-timers! registry (BasicHttpRequest. "GET" url2) (into-array ["foo" "abc"]))
    (testing "getClientMetrics without args returns all timers organized by category"
      (is (= (set ["url" "url-and-method" "metric-id"])
             (set (keys (Metrics/getClientMetrics registry)))
             (set (keys (Metrics/getClientMetricsData registry)))))
      (is (= (set [:url :url-and-method :metric-id])
             (set (keys (metrics/get-client-metrics registry)))
             (set (keys (metrics/get-client-metrics-data registry)))))
      (is (= (set
              [(add-metric-ns "with-url.http://test.com/one.bytes-read")
               (add-metric-ns "with-url.http://test.com/one/two.bytes-read")])
             (set (map #(.getMetricName %) (get (Metrics/getClientMetrics registry) "url")))
             (set (map #(.getMetricName %) (:url (metrics/get-client-metrics registry))))
             (set (map #(.getMetricName %) (get (Metrics/getClientMetricsData registry) "url")))
             (set (map :metric-name (:url (metrics/get-client-metrics-data registry))))))
      (is (= (set
              [(add-metric-ns "with-url-and-method.http://test.com/one.GET.bytes-read")
               (add-metric-ns "with-url-and-method.http://test.com/one.POST.bytes-read")
               (add-metric-ns "with-url-and-method.http://test.com/one/two.GET.bytes-read")])
             (set (map #(.getMetricName %)
                       (get (Metrics/getClientMetrics registry) "url-and-method")))
             (set (map #(.getMetricName %)
                       (:url-and-method (metrics/get-client-metrics registry))))
             (set (map #(.getMetricName %)
                       (get (Metrics/getClientMetricsData registry) "url-and-method")))
             (set (map :metric-name
                       (:url-and-method (metrics/get-client-metrics-data registry))))))
      (is (= (set ["puppetlabs.http-client.experimental.with-metric-id.foo.bytes-read"
                   "puppetlabs.http-client.experimental.with-metric-id.foo.bar.bytes-read"
                   "puppetlabs.http-client.experimental.with-metric-id.foo.abc.bytes-read"])
             (set (map #(.getMetricName %)
                       (get (Metrics/getClientMetrics registry) "metric-id")))
             (set (map #(.getMetricName %)
                       (:metric-id (metrics/get-client-metrics registry))))
             (set (map #(.getMetricName %)
                       (get (Metrics/getClientMetricsData registry) "metric-id")))
             (set (map :metric-name
                       (:metric-id (metrics/get-client-metrics-data registry)))))))
    (testing "getClientMetricsData with url returns the right thing"
      (let [java-data (Metrics/getClientMetricsDataByUrl registry url bytes-read)
            clj-data (metrics/get-client-metrics-data-by-url registry url)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url.http://test.com/one.bytes-read")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 3 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByUrl registry url2 bytes-read)
            clj-data (metrics/get-client-metrics-data-by-url registry url2)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url.http://test.com/one/two.bytes-read")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (testing "getClientMetricsData with url returns nothing if url is not a full match"
        (is (= [] (Metrics/getClientMetricsDataByUrl registry "http://test.com" bytes-read)
               (metrics/get-client-metrics-data-by-url registry "http://test.com")))))
    (testing "getClientMetricsData with url and method returns the right thing"
      (let [java-data (Metrics/getClientMetricsDataByUrlAndMethod registry url "GET" bytes-read)
            clj-data (metrics/get-client-metrics-data-by-url-and-method registry url :get)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url-and-method.http://test.com/one.GET.bytes-read")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByUrlAndMethod
                       registry url "POST" bytes-read)
            clj-data (metrics/get-client-metrics-data-by-url-and-method
                      registry url :post)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url-and-method.http://test.com/one.POST.bytes-read")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 2 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByUrlAndMethod
                       registry url2 "GET" bytes-read)
            clj-data (metrics/get-client-metrics-data-by-url-and-method registry url2 :get)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url-and-method.http://test.com/one/two.GET.bytes-read")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (testing "getClientMetricsData with url and method returns nothing if method is not a match"
        (is (= [] (Metrics/getClientMetricsDataByUrlAndMethod
                   registry "http://test.com" "PUT" bytes-read)
               (metrics/get-client-metrics-data-by-url-and-method registry "http://test.com" :put)))))
    (testing "getClientMetricsData with metric id returns the right thing"
      (let [java-data (Metrics/getClientMetricsDataByMetricId
                       registry (into-array ["foo"]) bytes-read)
            clj-data (metrics/get-client-metrics-data-by-metric-id registry ["foo"])]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-metric-id.foo.bytes-read")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 2 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByMetricId
                       registry (into-array ["foo" "bar"]) bytes-read)
            clj-data (metrics/get-client-metrics-data-by-metric-id
                      registry ["foo" "bar"])]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-metric-id.foo.bar.bytes-read")
               (.getMetricName  (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByMetricId
                       registry (into-array ["foo" "abc"]) bytes-read)
            clj-data (metrics/get-client-metrics-data-by-metric-id
                      registry ["foo" "abc"])]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-metric-id.foo.abc.bytes-read")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data))))
        (testing "metric id can be specified as keyword or string"
          (is (= clj-data
                 (metrics/get-client-metrics-data-by-metric-id registry ["foo" :abc])))))
      (testing "getClientMetricsData with metric id returns nothing if id is not a match"
        (is (= [] (Metrics/getClientMetricsDataByMetricId
                   registry (into-array ["foo" "cat"]) bytes-read)
               (metrics/get-client-metrics-data-by-metric-id registry ["foo" "cat"]))))
      (testing "getClientMetrics|Data returns nil if no metric registry passed in"
        (is (= nil (Metrics/getClientMetrics nil) (Metrics/getClientMetricsData nil)))
        (is (= nil (metrics/get-client-metrics nil) (metrics/get-client-metrics-data nil))))
      (testing (str "getClientMetrics|Data returns map with empty arrays as values"
                    " if no requests have been made yet")
        (is (= {"url" [] "url-and-method" [] "metric-id" []}
               (into {} (Metrics/getClientMetrics (MetricRegistry.)))
               (into {} (Metrics/getClientMetricsData (MetricRegistry.)))))
        (is (= {:url [] :url-and-method [] :metric-id []}
               (metrics/get-client-metrics (MetricRegistry.))
               (metrics/get-client-metrics-data (MetricRegistry.)))))
      (testing "getClientMetrics returns correctly without metric-id on request"
        (let [registry (MetricRegistry.)
              url "http://test.com/one"]
          (start-and-stop-timers! registry (BasicHttpRequest. "GET" url) nil)
          (let [client-metrics (Metrics/getClientMetrics registry)
                client-metrics-data (Metrics/getClientMetricsData registry)]
            (is (= (set ["url" "url-and-method" "metric-id"])
                   (set (keys client-metrics))
                   (set (keys client-metrics-data))))
            (is (= (set [(add-metric-ns
                          "with-url.http://test.com/one.bytes-read")])
                   (set (map #(.getMetricName %) (get client-metrics "url")))
                   (set (map #(.getMetricName %) (get client-metrics-data "url")))))
            (is (= (set [(add-metric-ns
                          "with-url-and-method.http://test.com/one.GET.bytes-read")])
                   (set (map #(.getMetricName %)
                             (get client-metrics "url-and-method")))
                   (set (map #(.getMetricName %)
                             (get client-metrics-data "url-and-method")))))
            (is (= []
                   (get client-metrics "metric-id")
                   (get client-metrics-data "metric-id")))))))))

(deftest empty-metric-id-filter-test
  (testing "a metric id filter with an empty array returns all metric id timers"
    (let [registry (MetricRegistry.)
          url "http://test.com/foo/bar"
          foo-id (add-metric-ns "with-metric-id.foo.bytes-read")
          foo-bar-id (add-metric-ns "with-metric-id.foo.bar.bytes-read")
          foo-bar-baz-id (add-metric-ns "with-metric-id.foo.bar.baz.bytes-read")]
      (start-and-stop-timers! registry (BasicHttpRequest. "GET" url)
                              (into-array ["foo" "bar" "baz"]))
      (testing "empty metric filter returns all metric id timers"
        (is (= (set (list foo-id foo-bar-id foo-bar-baz-id))
               (set (map #(.getMetricName %)
                         (Metrics/getClientMetricsDataByMetricId
                          registry (into-array String []) bytes-read)))
               (set (map :metric-name
                         (metrics/get-client-metrics-data-by-metric-id registry [])))))))))
