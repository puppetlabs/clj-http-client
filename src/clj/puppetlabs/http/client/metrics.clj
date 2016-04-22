(ns puppetlabs.http.client.metrics
  (:require [schema.core :as schema]
            [puppetlabs.http.client.common :as common])
  (:import (com.puppetlabs.http.client.impl Metrics$MetricType Metrics
                                            ClientMetricData)))

(schema/defn get-metric-data :- common/MetricData
  [data :- ClientMetricData]
  {:count (.getCount data)
   :mean (.getMean data)
   :aggregate (.getAggregate data)
   :metric-name (.getMetricName data)
   :url (.getUrl data)
   :method (.getMethod data)
   :metric-id (.getMetricId data)})

(defn get-java-metric-type
  [metric-type]
  (case metric-type
    :full-response Metrics$MetricType/FULL_RESPONSE))

(defn uppercase-method
  [method]
  (clojure.string/upper-case (name method)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn ^:always-validate get-client-metrics
  :- (schema/maybe common/AllMetrics)
  "Returns the http client-specific metrics from the metric registry."
  [metric-registry :- common/OptionalMetricRegistry]
  (when metric-registry
    (let [metrics (Metrics/getClientMetrics metric-registry)]
      {:url (get metrics "url")
       :url-and-method (get metrics "url-and-method")
       :metric-id (get metrics "metric-id")})))

(schema/defn ^:always-validate get-client-metrics-by-url
  :- common/Metrics
  "Returns the http client-specific url metrics matching the specified url."
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str]
   (get-client-metrics-by-url metric-registry url :full-response))
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str
    metric-type :- common/MetricTypes]
   (when metric-registry
     (Metrics/getClientMetricsByUrl
      metric-registry
      url
      (get-java-metric-type metric-type)))))

(schema/defn ^:always-validate get-client-metrics-by-url-and-method
  :- common/Metrics
  "Returns the http client-specific url metrics matching the specified url."
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str
    method :- common/HTTPMethod]
   (get-client-metrics-by-url-and-method metric-registry url method :full-response))
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str
    method :- common/HTTPMethod
    metric-type :- common/MetricTypes]
   (when metric-registry
     (Metrics/getClientMetricsByUrlAndMethod
      metric-registry
      url
      method
      (get-java-metric-type metric-type)))))

(schema/defn ^:always-validate get-client-metrics-by-metric-id
  :- common/Metrics
  "Returns the http client-specific url metrics matching the specified url."
  ([metric-registry :- common/OptionalMetricRegistry
    metric-id :- common/MetricId]
   (get-client-metrics-by-metric-id metric-registry metric-id :full-response))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-id :- common/MetricId
    metric-type :- common/MetricTypes]
   (when metric-registry
     (Metrics/getClientMetricsByMetricId
      metric-registry
      (into-array String (map name metric-id))
      (get-java-metric-type metric-type)))))

(schema/defn ^:always-validate get-client-metrics-data
  :- (schema/maybe common/AllMetricsData)
  "Returns a summary of the metric data for all http client timers, organized
  in a map by category."
  ([metric-registry :- common/OptionalMetricRegistry]
   (get-client-metrics-data metric-registry :full-response))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-type :- common/MetricTypes]
   (when metric-registry
     (let [data (Metrics/getClientMetricsData
                 metric-registry
                 (get-java-metric-type metric-type))]
       {:url (map get-metric-data (get data "url"))
        :url-and-method (map get-metric-data (get data "url-and-method"))
        :metric-id (map get-metric-data (get data "metric-id"))}))))

(schema/defn ^:always-validate get-client-metrics-data-by-url
  :- common/MetricsData
  "Returns a summary of the metric data for all http client timers filtered by
  url."
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str]
   (get-client-metrics-data-by-url metric-registry url :full-response))
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str
    metric-type :- common/MetricTypes]
   (when metric-registry
     (let [data (Metrics/getClientMetricsDataByUrl
                 metric-registry
                 url
                 (get-java-metric-type metric-type))]
       (map get-metric-data data)))))

(schema/defn ^:always-validate get-client-metrics-data-by-url-and-method
  :- common/MetricsData
  "Returns a summary of the metric data for all http client timers filtered by
  url and method."
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str
    method :- common/HTTPMethod]
   (get-client-metrics-data-by-url-and-method metric-registry url method :full-response))
  ([metric-registry :- common/OptionalMetricRegistry
    url :- schema/Str
    method :- common/HTTPMethod
    metric-type :- common/MetricTypes]
   (when metric-registry
     (let [data (Metrics/getClientMetricsDataByUrlAndMethod
                 metric-registry
                 url
                 (uppercase-method method)
                 (get-java-metric-type metric-type))]
       (map get-metric-data data)))))

(schema/defn ^:always-validate get-client-metrics-data-by-metric-id
  :- common/MetricsData
  "Returns a summary of the metric data for all http client timers filtered by
  metric-id."
  ([metric-registry :- common/OptionalMetricRegistry
    metric-id :- common/MetricId]
   (get-client-metrics-data-by-metric-id metric-registry metric-id :full-response))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-id :- common/MetricId
    metric-type :- common/MetricTypes]
   (when metric-registry
     (let [data (Metrics/getClientMetricsDataByMetricId
                 metric-registry
                 (into-array String (map name metric-id))
                 (get-java-metric-type metric-type))]
       (map get-metric-data data)))))
