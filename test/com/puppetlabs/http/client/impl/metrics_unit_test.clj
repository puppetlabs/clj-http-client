(ns com.puppetlabs.http.client.impl.metrics-unit-test
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.metrics :as metrics])
  (:import (com.codahale.metrics MetricRegistry)
           (com.puppetlabs.http.client.impl Metrics Metrics$MetricType ClientMetricRegistry)
           (org.apache.http.message BasicHttpRequest)))

(def bytes-read Metrics$MetricType/BYTES_READ)

(defn add-metric-ns [string]
  (str "puppetlabs.http-client.experimental." string))

(deftest start-bytes-read-timers-test
  (testing "startBytesReadTimers creates the right timers"
    (let [url-id (add-metric-ns "with-url.http://localhost/foo.bytes-read")
          url-method-id (add-metric-ns "with-url.http://localhost/foo.GET.bytes-read")]
      (testing "metric id timers are not created for a request without a metric id"
        (let [metric-registry (ClientMetricRegistry. (MetricRegistry.))]
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://localhost/foo")
                                        nil)
          (is (= (set (list url-id url-method-id)) (set (keys (.getTimers metric-registry)))))))
      (testing "metric id timers are not created for a request with an empty metric id"
        (let [metric-registry (ClientMetricRegistry. (MetricRegistry.))]
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://localhost/foo")
                                        (into-array String []))
          (is (= (set (list url-id url-method-id)) (set (keys (.getTimers metric-registry)))))))
      (testing "metric id timers are created correctly for a request with a metric id"
        (let [metric-registry (ClientMetricRegistry. (MetricRegistry.))]
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://localhost/foo")
                                        (into-array ["foo" "bar" "baz"]))
          (is (= (set (list url-id url-method-id
                            (add-metric-ns "with-metric-id.foo.bytes-read")
                            (add-metric-ns "with-metric-id.foo.bar.bytes-read")
                            (add-metric-ns "with-metric-id.foo.bar.baz.bytes-read")))
                 (set (keys (.getTimers metric-registry)))))))
      (testing "url timers should strip off username, password, query string, and fragment"
        (let [metric-registry (ClientMetricRegistry. (MetricRegistry.))]
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?te%2cst=one")
                                        nil)
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz#x%2cyz")
                                        nil)
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?te%2cst=one#x%2cyz")
                                        nil)
          (Metrics/startBytesReadTimers metric-registry
                                        (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?#x%2cyz")
                                        nil)
          (is (= (set (list (add-metric-ns "with-url.http://localhost:1234/foo%2cbar/baz.bytes-read")
                            (add-metric-ns "with-url.http://localhost:1234/foo%2cbar/baz.GET.bytes-read")))
                 (set (keys (.getTimers metric-registry))))))))))

(defn start-and-stop-timers! [registry req id]
  (doseq [timer (Metrics/startBytesReadTimers
                 registry
                 req
                 id)]
    (.stop timer)))

(deftest get-client-metrics-data-test
  (let [registry (ClientMetricRegistry. (MetricRegistry.))
        url "http://test.com/one"
        url2 "http://test.com/one/two"]
    (start-and-stop-timers! registry (BasicHttpRequest. "GET" url) nil)
    (start-and-stop-timers! registry (BasicHttpRequest. "POST" url) nil)
    (start-and-stop-timers! registry (BasicHttpRequest. "POST" url) (into-array ["foo" "bar"]))
    (start-and-stop-timers! registry (BasicHttpRequest. "GET" url2) (into-array ["foo" "abc"]))
    (testing "getClientMetrics without args returns all timers"
      (is (= (set
              ["puppetlabs.http-client.experimental.with-url.http://test.com/one.bytes-read"
               "puppetlabs.http-client.experimental.with-url.http://test.com/one.GET.bytes-read"
               "puppetlabs.http-client.experimental.with-url.http://test.com/one.POST.bytes-read"
               "puppetlabs.http-client.experimental.with-metric-id.foo.bytes-read"
               "puppetlabs.http-client.experimental.with-metric-id.foo.bar.bytes-read"
               "puppetlabs.http-client.experimental.with-url.http://test.com/one/two.bytes-read"
               "puppetlabs.http-client.experimental.with-url.http://test.com/one/two.GET.bytes-read"
               "puppetlabs.http-client.experimental.with-metric-id.foo.abc.bytes-read"])
             (set (keys (Metrics/getClientMetrics registry)))
             (set (keys (Metrics/getClientMetricsData registry))))))
    (testing "getClientMetricsData with url returns the right thing"
      (let [java-data (Metrics/getClientMetricsData registry url bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:url url :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-url.http://test.com/one.bytes-read")
               (first (keys java-data))
               (first (keys clj-data))))
        (is (= 3 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data))))))
      (let [java-data (Metrics/getClientMetricsData registry url2 bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:url url2 :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-url.http://test.com/one/two.bytes-read")
               (first (keys java-data))
               (first (keys clj-data))))
        (is (= 1 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data))))))
      (testing "getClientMetricsData with url returns nothing if url is not a full match"
        (is (= {} (Metrics/getClientMetricsData registry "http://test.com" bytes-read)
               (metrics/get-client-metrics-data
                registry {:url "http://test.com" :metric-type :bytes-read})))))
    (testing "getClientMetricsData with url and method returns the right thing"
      (let [java-data (Metrics/getClientMetricsData registry url "GET" bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:url url :method :get :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-url.http://test.com/one.GET.bytes-read")
               (first (keys clj-data))
               (first (keys java-data))))
        (is (= 1 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data))))))
      (let [java-data (Metrics/getClientMetricsData registry url "POST" bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:url url :method :post :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-url.http://test.com/one.POST.bytes-read")
               (first (keys java-data))
               (first (keys clj-data))))
        (is (= 2 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data))))))
      (let [java-data (Metrics/getClientMetricsData registry url2 "GET" bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:url url2 :method :get :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-url.http://test.com/one/two.GET.bytes-read")
               (first (keys java-data))
               (first (keys clj-data))))
        (is (= 1 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data))))))
      (testing "getClientMetricsData with url and method returns nothing if method is not a match"
        (is (= {} (Metrics/getClientMetricsData
                   registry "http://test.com" "PUT" bytes-read)
               (metrics/get-client-metrics-data
                registry {:url "http://test.com" :method :put :metric-type :bytes-read})))))
    (testing "getClientMetricsData with metric id returns the right thing"
      (let [java-data (Metrics/getClientMetricsData
                       registry (into-array ["foo"]) bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:metric-id ["foo"] :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-metric-id.foo.bytes-read")
               (first (keys java-data))
               (first (keys clj-data))))
        (is (= 2 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data))))))
      (let [java-data (Metrics/getClientMetricsData
                       registry (into-array ["foo" "bar"]) bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:metric-id ["foo" "bar"] :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-metric-id.foo.bar.bytes-read")
               (first (keys java-data))
               (first (keys clj-data))))
        (is (= 1 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data))))))
      (let [java-data (Metrics/getClientMetricsData
                       registry (into-array ["foo" "abc"]) bytes-read)
            clj-data (metrics/get-client-metrics-data
                      registry {:metric-id ["foo" "abc"] :metric-type :bytes-read})]
        (is (= (add-metric-ns "with-metric-id.foo.abc.bytes-read")
               (first (keys java-data))
               (first (keys clj-data))))
        (is (= 1 (.getCount (first (vals java-data)))
               (:count (first (vals clj-data)))))
        (testing "metric id can be specified as keyword or string"
          (is (= clj-data
                 (metrics/get-client-metrics-data
                  registry {:metric-id ["foo" :abc] :metric-type :bytes-read})))))
      (testing "getClientMetricsData with metric id returns nothing if id is not a match"
        (is (= {} (Metrics/getClientMetricsData
                   registry (into-array ["foo" "cat"]) bytes-read)
               (metrics/get-client-metrics-data
                registry {:metric-id ["foo" "cat"] :metric-type :bytes-read}))))
      (testing "getClientMetrics|Data returns nil if no metric registry passed in"
        (is (= nil (Metrics/getClientMetricsData nil) (Metrics/getClientMetrics nil)))
        (is (= nil (metrics/get-client-metrics-data nil) (metrics/get-client-metrics nil)))))))

(deftest empty-metric-id-filter-test
  (testing "a metric id filter with an empty array returns all metric id timers"
    (let [registry (ClientMetricRegistry. (MetricRegistry.))
          url "http://test.com/foo/bar"
          foo-id (add-metric-ns "with-metric-id.foo.bytes-read")
          foo-bar-id (add-metric-ns "with-metric-id.foo.bar.bytes-read")
          foo-bar-baz-id (add-metric-ns "with-metric-id.foo.bar.baz.bytes-read")]
      (start-and-stop-timers! registry (BasicHttpRequest. "GET" url) (into-array ["foo" "bar" "baz"]))
      (testing "empty metric filter returns all metric id timers"
        (is (= (set (list foo-id foo-bar-id foo-bar-baz-id))
               (set (keys (Metrics/getClientMetricsData registry (into-array String []) bytes-read)))
               (set (keys (metrics/get-client-metrics-data registry (metrics/filter-with-metric-id []))))))))))

(deftest metrics-filter-builder-test
  (let [metric-registry (ClientMetricRegistry. (MetricRegistry.))
        url "http://test.com/foo/bar"]
    (start-and-stop-timers! metric-registry (BasicHttpRequest. "GET" url) (into-array ["foo" "bar"]))
    (testing "url-filter works"
      (is (= (metrics/get-client-metrics-data metric-registry {:url url :metric-type :bytes-read})
             (metrics/get-client-metrics-data metric-registry (metrics/filter-with-url url)))))
    (testing "url-method-filter works"
      (is (= (metrics/get-client-metrics-data metric-registry {:url url :method :get :metric-type :bytes-read})
             (metrics/get-client-metrics-data metric-registry (metrics/filter-with-url-and-method url :get)))))
    (testing "metric-id-filter works"
      (is (= (metrics/get-client-metrics-data metric-registry {:metric-id [:foo :bar] :metric-type :bytes-read})
             (metrics/get-client-metrics-data metric-registry (metrics/filter-with-metric-id [:foo :bar])))))))
