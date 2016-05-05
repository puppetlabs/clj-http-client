(ns puppetlabs.http.client.metrics
  (:require [schema.core :as schema]
            [puppetlabs.http.client.common :as common])
  (:import (com.puppetlabs.http.client.metrics Metrics$MetricType Metrics
                                            ClientMetricData)
           (com.codahale.metrics MetricRegistry)))

(schema/defn get-base-metric-data :- common/BaseMetricData
  [data :- ClientMetricData]
  {:count (.getCount data)
   :mean (.getMean data)
   :aggregate (.getAggregate data)
   :metric-name (.getMetricName data)})

(schema/defn get-url-metric-data :- common/UrlMetricData
  [data :- ClientMetricData]
  (assoc (get-base-metric-data data) :url (.getUrl data)))

(schema/defn get-url-and-method-metric-data :- common/UrlAndMethodMetricData
  [data :- ClientMetricData]
  (assoc (get-url-metric-data data) :method (.getMethod data)))

(schema/defn get-metric-id-metric-data :- common/MetricIdMetricData
  [data :- ClientMetricData]
  (assoc (get-base-metric-data data) :metric-id (.getMetricId data)))

(defn get-java-metric-type
  [metric-type]
  (case metric-type
    :full-response Metrics$MetricType/FULL_RESPONSE))

(defn uppercase-method
  [method]
  (clojure.string/upper-case (name method)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn ^:always-validate url->metric-url :- schema/Str
  [url :- schema/Str]
 (Metrics/urlToMetricUrl url))

(schema/defn ^:always-validate get-client-metrics
  :- (schema/maybe common/AllMetrics)
  "Returns the http client-specific metrics from the metric registry."
  [metric-registry :- MetricRegistry]
  (let [metrics (Metrics/getClientMetrics metric-registry)]
    {:url (.getUrlTimers metrics)
     :url-and-method (.getUrlAndMethodTimers metrics)
     :metric-id (.getMetricIdTimers metrics)}))

(schema/defn ^:always-validate get-client-metrics-by-url
  :- common/Metrics
  "Returns the http client-specific url metrics matching the specified url."
  [metric-registry :- MetricRegistry
   url :- schema/Str]
  (Metrics/getClientMetricsByUrl
   metric-registry
   url))

(schema/defn ^:always-validate get-client-metrics-by-url-and-method
  :- common/Metrics
  "Returns the http client-specific url metrics matching the specified url."
  [metric-registry :- MetricRegistry
   url :- schema/Str
   method :- common/HTTPMethod]
  (Metrics/getClientMetricsByUrlAndMethod
   metric-registry
   url
   method))

(schema/defn ^:always-validate get-client-metrics-by-metric-id
  :- common/Metrics
  "Returns the http client-specific url metrics matching the specified url."
  [metric-registry :- MetricRegistry
   metric-id :- common/MetricId]
  (Metrics/getClientMetricsByMetricId
   metric-registry
   (into-array String (map name metric-id))))

(schema/defn ^:always-validate get-client-metrics-data
  :- common/AllMetricsData
  "Returns a summary of the metric data for all http client timers, organized
  in a map by category."
  [metric-registry :- MetricRegistry]
  (let [data (Metrics/getClientMetricsData metric-registry)]
    {:url (map get-url-metric-data (.getUrlData data))
     :url-and-method (map get-url-and-method-metric-data (.getUrlAndMethodData data))
     :metric-id (map get-metric-id-metric-data (.getMetricIdData data))}))

(schema/defn ^:always-validate get-client-metrics-data-by-url
  :- [common/UrlMetricData]
  "Returns a summary of the metric data for all http client timers filtered by
  url."
  [metric-registry :- MetricRegistry
   url :- schema/Str]
  (let [data (Metrics/getClientMetricsDataByUrl
              metric-registry
              url)]
    (map get-url-metric-data data)))

(schema/defn ^:always-validate get-client-metrics-data-by-url-and-method
  :- [common/UrlAndMethodMetricData]
  "Returns a summary of the metric data for all http client timers filtered by
  url and method."
  [metric-registry :- MetricRegistry
   url :- schema/Str
   method :- common/HTTPMethod]
  (let [data (Metrics/getClientMetricsDataByUrlAndMethod
              metric-registry
              url
              (uppercase-method method))]
    (map get-url-and-method-metric-data data)))

(schema/defn ^:always-validate get-client-metrics-data-by-metric-id
  :- [common/MetricIdMetricData]
  "Returns a summary of the metric data for all http client timers filtered by
  metric-id."
  [metric-registry :- MetricRegistry
   metric-id :- common/MetricId]
  (let [data (Metrics/getClientMetricsDataByMetricId
              metric-registry
              (into-array String (map name metric-id)))]
    (map get-metric-id-metric-data data)))
