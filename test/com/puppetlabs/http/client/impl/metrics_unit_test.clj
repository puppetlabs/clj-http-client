(ns com.puppetlabs.http.client.impl.metrics-unit-test
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.metrics :as metrics]
            [schema.test :as schema-test])
  (:import (com.codahale.metrics MetricRegistry)
           (com.puppetlabs.http.client.metrics Metrics)
           (org.apache.http.message BasicHttpRequest)
           (clojure.lang ExceptionInfo)
           (com.puppetlabs.http.client.impl.metrics TimerUtils)
           (java.net URISyntaxException)))

(use-fixtures :once schema-test/validate-schemas)

(defn add-metric-ns [string]
  (str "puppetlabs.http-client.experimental." string))

(deftest start-full-response-timers-test
  (testing "startFullResponseTimers creates the right timers"
    (let [url-id (add-metric-ns "with-url.http://localhost/foo.full-response")
          url-method-id (add-metric-ns "with-url-and-method.http://localhost/foo.GET.full-response")]
      (testing "metric id timers are not created for a request without a metric id"
        (let [metric-registry (MetricRegistry.)]
          (TimerUtils/startFullResponseTimers metric-registry
                                           (BasicHttpRequest. "GET" "http://localhost/foo")
                                           nil)
          (is (= (set (list url-id url-method-id)) (set (keys (.getTimers metric-registry)))))))
      (testing "metric id timers are not created for a request with an empty metric id"
        (let [metric-registry (MetricRegistry.)]
          (TimerUtils/startFullResponseTimers metric-registry
                                           (BasicHttpRequest. "GET" "http://localhost/foo")
                                           (into-array String []))
          (is (= (set (list url-id url-method-id)) (set (keys (.getTimers metric-registry)))))))
      (testing "metric id timers are created correctly for a request with a metric id"
        (let [metric-registry (MetricRegistry.)]
          (TimerUtils/startFullResponseTimers metric-registry
                                              (BasicHttpRequest. "GET" "http://localhost/foo")
                                              (into-array ["foo" "bar" "baz"]))
          (is (= (set (list url-id url-method-id
                            (add-metric-ns "with-metric-id.foo.full-response")
                            (add-metric-ns "with-metric-id.foo.bar.full-response")
                            (add-metric-ns "with-metric-id.foo.bar.baz.full-response")))
                 (set (keys (.getTimers metric-registry)))))))
      (testing "url timers should strip off username, password, query string, and fragment"
        (let [metric-registry (MetricRegistry.)]
          (TimerUtils/startFullResponseTimers
           metric-registry
           (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?te%2cst=one")
           nil)
          (TimerUtils/startFullResponseTimers
           metric-registry
           (BasicHttpRequest. "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz#x%2cyz")
           nil)
          (TimerUtils/startFullResponseTimers
           metric-registry
           (BasicHttpRequest.
            "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?te%2cst=one#x%2cyz")
           nil)
          (TimerUtils/startFullResponseTimers
           metric-registry
           (BasicHttpRequest.
            "GET" "http://user:pwd@localhost:1234/foo%2cbar/baz?#x%2cyz")
           nil)
          (is (= (set (list
                       (add-metric-ns
                        "with-url.http://localhost:1234/foo,bar/baz.full-response")
                       (add-metric-ns
                        "with-url-and-method.http://localhost:1234/foo,bar/baz.GET.full-response")))
                 (set (keys (.getTimers metric-registry))))))))))

(deftest url->metric-url-test
  (testing "url->metric-url strips username, password, query params, and path fragment off of url"
    (let [url "http://user:pwd@localhost:1234/foo%2cbar/baz?te%2cst=one"]
      (is (= "http://localhost:1234/foo,bar/baz"
             (Metrics/urlToMetricUrl url)
             (metrics/url->metric-url url)))))
  (testing "url->metric-url throws error if non-url passed in"
    (is (thrown? URISyntaxException (Metrics/urlToMetricUrl "abc def")))
    (is (thrown? URISyntaxException (metrics/url->metric-url "abc def")))))

(defn start-and-stop-timers! [registry req id]
  (doseq [timer (TimerUtils/startFullResponseTimers
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
      (is (= (set [:url :url-and-method :metric-id])
             (set (keys (metrics/get-client-metrics registry)))
             (set (keys (metrics/get-client-metrics-data registry)))))
      (is (= (set
              [(add-metric-ns "with-url.http://test.com/one.full-response")
               (add-metric-ns "with-url.http://test.com/one/two.full-response")])
             (set (map #(.getMetricName %) (.getUrlTimers (Metrics/getClientMetrics registry))))
             (set (map #(.getMetricName %) (:url (metrics/get-client-metrics registry))))
             (set (map #(.getMetricName %) (.getUrlData (Metrics/getClientMetricsData registry))))
             (set (map :metric-name (:url (metrics/get-client-metrics-data registry))))))
      (is (= (set
              [(add-metric-ns "with-url-and-method.http://test.com/one.GET.full-response")
               (add-metric-ns "with-url-and-method.http://test.com/one.POST.full-response")
               (add-metric-ns "with-url-and-method.http://test.com/one/two.GET.full-response")])
             (set (map #(.getMetricName %)
                       (.getUrlAndMethodTimers (Metrics/getClientMetrics registry))))
             (set (map #(.getMetricName %)
                       (:url-and-method (metrics/get-client-metrics registry))))
             (set (map #(.getMetricName %)
                       (.getUrlAndMethodData (Metrics/getClientMetricsData registry))))
             (set (map :metric-name
                       (:url-and-method (metrics/get-client-metrics-data registry))))))
      (is (= (set ["puppetlabs.http-client.experimental.with-metric-id.foo.full-response"
                   "puppetlabs.http-client.experimental.with-metric-id.foo.bar.full-response"
                   "puppetlabs.http-client.experimental.with-metric-id.foo.abc.full-response"])
             (set (map #(.getMetricName %)
                       (.getMetricIdTimers (Metrics/getClientMetrics registry))))
             (set (map #(.getMetricName %)
                       (:metric-id (metrics/get-client-metrics registry))))
             (set (map #(.getMetricName %)
                       (.getMetricIdData (Metrics/getClientMetricsData registry))))
             (set (map :metric-name
                       (:metric-id (metrics/get-client-metrics-data registry)))))))
    (testing "getClientMetricsData with url returns the right thing"
      (let [java-data (Metrics/getClientMetricsDataByUrl registry url)
            clj-data (metrics/get-client-metrics-data-by-url registry url)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url.http://test.com/one.full-response")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 3 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByUrl registry url2)
            clj-data (metrics/get-client-metrics-data-by-url registry url2)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url.http://test.com/one/two.full-response")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (testing "getClientMetricsData with url returns nothing if url is not a full match"
        (is (= [] (Metrics/getClientMetricsDataByUrl registry "http://test.com")
               (metrics/get-client-metrics-data-by-url registry "http://test.com")))))
    (testing "getClientMetricsData with url and method returns the right thing"
      (let [java-data (Metrics/getClientMetricsDataByUrlAndMethod registry url "GET")
            clj-data (metrics/get-client-metrics-data-by-url-and-method registry url :get)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url-and-method.http://test.com/one.GET.full-response")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByUrlAndMethod registry url "POST")
            clj-data (metrics/get-client-metrics-data-by-url-and-method
                      registry url :post)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url-and-method.http://test.com/one.POST.full-response")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 2 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByUrlAndMethod registry url2 "GET")
            clj-data (metrics/get-client-metrics-data-by-url-and-method registry url2 :get)]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-url-and-method.http://test.com/one/two.GET.full-response")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (testing "getClientMetricsData with url and method returns nothing if method is not a match"
        (is (= [] (Metrics/getClientMetricsDataByUrlAndMethod registry "http://test.com" "PUT")
               (metrics/get-client-metrics-data-by-url-and-method registry "http://test.com" :put)))))
    (testing "getClientMetricsData with metric id returns the right thing"
      (let [java-data (Metrics/getClientMetricsDataByMetricId registry (into-array ["foo"]))
            clj-data (metrics/get-client-metrics-data-by-metric-id registry ["foo"])]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-metric-id.foo.full-response")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 2 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByMetricId registry (into-array ["foo" "bar"]))
            clj-data (metrics/get-client-metrics-data-by-metric-id
                      registry ["foo" "bar"])]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-metric-id.foo.bar.full-response")
               (.getMetricName  (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data)))))
      (let [java-data (Metrics/getClientMetricsDataByMetricId registry (into-array ["foo" "abc"]))
            clj-data (metrics/get-client-metrics-data-by-metric-id
                      registry ["foo" "abc"])]
        (is (= 1 (count java-data) (count clj-data)))
        (is (= (add-metric-ns "with-metric-id.foo.abc.full-response")
               (.getMetricName (first java-data))
               (:metric-name (first clj-data))))
        (is (= 1 (.getCount (first java-data))
               (:count (first clj-data))))
        (testing "metric id can be specified as keyword or string"
          (is (= clj-data
                 (metrics/get-client-metrics-data-by-metric-id registry ["foo" :abc])))))
      (testing "getClientMetricsData with metric id returns nothing if id is not a match"
        (is (= [] (Metrics/getClientMetricsDataByMetricId registry (into-array ["foo" "cat"]))
               (metrics/get-client-metrics-data-by-metric-id registry ["foo" "cat"]))))
      (testing "getClientMetrics|Data returns throws an error if no metric registry passed in"
        (is (thrown? ExceptionInfo (metrics/get-client-metrics nil)))
        (is (thrown? ExceptionInfo (metrics/get-client-metrics-data nil)))
        (is (thrown? IllegalArgumentException (Metrics/getClientMetrics nil)))
        (is (thrown? IllegalArgumentException (Metrics/getClientMetricsData nil))))
      (testing (str "getClientMetrics|Data returns data structure with empty arrays"
                    "  as values if no requests have been made yet")
        (let [empty-metrics (Metrics/getClientMetrics (MetricRegistry.))
              empty-metrics-data (Metrics/getClientMetricsData (MetricRegistry.))]
          (is (= {:url [] :url-and-method [] :metric-id []}
                 (metrics/get-client-metrics (MetricRegistry.))
                 (metrics/get-client-metrics-data (MetricRegistry.))))
          (is (empty? (.getUrlTimers empty-metrics)))
          (is (empty? (.getUrlAndMethodTimers empty-metrics)))
          (is (empty? (.getMetricIdTimers empty-metrics)))
          (is (empty? (.getUrlData empty-metrics-data)))
          (is (empty? (.getUrlAndMethodData empty-metrics-data)))
          (is (empty? (.getMetricIdData empty-metrics-data)))))
      (testing "getClientMetrics returns correctly without metric-id on request"
        (let [registry (MetricRegistry.)
              url "http://test.com/one"]
          (start-and-stop-timers! registry (BasicHttpRequest. "GET" url) nil)
          (let [client-metrics (Metrics/getClientMetrics registry)
                client-metrics-data (Metrics/getClientMetricsData registry)]
            (is (= (set [(add-metric-ns
                          "with-url.http://test.com/one.full-response")])
                   (set (map #(.getMetricName %) (.getUrlTimers client-metrics)))
                   (set (map #(.getMetricName %) (.getUrlData client-metrics-data)))))
            (is (= (set [(add-metric-ns
                          "with-url-and-method.http://test.com/one.GET.full-response")])
                   (set (map #(.getMetricName %)
                             (.getUrlAndMethodTimers client-metrics)))
                   (set (map #(.getMetricName %)
                             (.getUrlAndMethodData client-metrics-data)))))
            (is (= []
                   (.getMetricIdTimers client-metrics)
                   (.getMetricIdData client-metrics-data)))))))))

(deftest empty-metric-id-filter-test
  (testing "a metric id filter with an empty array returns all metric id timers"
    (let [registry (MetricRegistry.)
          url "http://test.com/foo/bar"
          foo-id (add-metric-ns "with-metric-id.foo.full-response")
          foo-bar-id (add-metric-ns "with-metric-id.foo.bar.full-response")
          foo-bar-baz-id (add-metric-ns "with-metric-id.foo.bar.baz.full-response")]
      (start-and-stop-timers! registry (BasicHttpRequest. "GET" url)
                              (into-array ["foo" "bar" "baz"]))
      (testing "empty metric filter returns all metric id timers"
        (is (= (set (list foo-id foo-bar-id foo-bar-baz-id))
               (set (map #(.getMetricName %)
                         (Metrics/getClientMetricsDataByMetricId registry (into-array String []))))
               (set (map :metric-name
                         (metrics/get-client-metrics-data-by-metric-id registry [])))))))))
