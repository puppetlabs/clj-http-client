(ns puppetlabs.http.client.metrics
  (:require [schema.core :as schema]
            [puppetlabs.http.client.common :as common])
  (:import (com.codahale.metrics Timer MetricRegistry)
           (java.util.concurrent TimeUnit)
           (com.puppetlabs.http.client.impl Metrics$MetricType Metrics ClientMetricRegistry)))

(schema/defn get-mean :- schema/Num
  [timer :- Timer]
  (->> timer
       .getSnapshot
       .getMean
       (.toMillis TimeUnit/NANOSECONDS)))

(defn get-metric-data
  [timer metric-name]
  (let [count (.getCount timer)
        mean (get-mean timer)
        aggregate (* count mean)]
    {:count count
     :mean mean
     :aggregate aggregate
     :metric-name metric-name}))

(defn get-metrics-data
  [timers]
  (reduce (fn [acc [metric-name timer]]
            (assoc acc metric-name (get-metric-data timer metric-name)))
          {} timers))

(defn get-java-metric-type
  [metric-type]
  (case metric-type
    :bytes-read Metrics$MetricType/BYTES_READ))

(defn uppercase-method
  [method]
  (clojure.string/upper-case (name method)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn ^:always-validate filter-with-metric-id :- common/MetricFilter
  [metric-id :- common/MetricId]
  {:metric-id metric-id
   :metric-type :bytes-read})

(schema/defn ^:always-validate filter-with-url :- common/MetricFilter
  [url :- schema/Str]
  {:url url
   :metric-type :bytes-read})

(schema/defn ^:always-validate filter-with-url-and-method :- common/MetricFilter
  [url :- schema/Str
   method :- common/HTTPMethod]
  {:url url
   :method method
   :metric-type :bytes-read})

(schema/defn ^:always-validate get-client-metrics :- (schema/maybe common/Metrics)
  "Returns the http client-specific metrics from the metric registry."
  ([metric-registry :- common/OptionalMetricRegistry]
   (when metric-registry
     (into {} (Metrics/getClientMetrics metric-registry))))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-filter :- common/MetricFilter]
   (when metric-registry
     (cond
       (:method metric-filter) (into {} (Metrics/getClientMetrics
                                         metric-registry
                                         (:url metric-filter)
                                         (uppercase-method (:method metric-filter))
                                         (get-java-metric-type (:metric-type metric-filter))))
       (:url metric-filter) (into {} (Metrics/getClientMetrics
                                      metric-registry
                                      (:url metric-filter)
                                      (get-java-metric-type (:metric-type metric-filter))))
       (:metric-id metric-filter) (into {} (Metrics/getClientMetrics
                                            metric-registry
                                            (into-array String (map name (:metric-id metric-filter)))
                                            (get-java-metric-type (:metric-type metric-filter))))
       :else (throw (IllegalArgumentException. "Not an allowed metric filter."))))))

(schema/defn ^:always-validate get-client-metrics-data :- (schema/maybe common/MetricsData)
  "Returns a map of metric-id to metric data summary."
  ([metric-registry :- common/OptionalMetricRegistry]
   (when metric-registry
     (let [timers (get-client-metrics metric-registry)]
       (get-metrics-data timers))))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-filter :- common/MetricFilter]
   (when metric-registry
     (let [timers (get-client-metrics metric-registry metric-filter)]
       (get-metrics-data timers)))))

(schema/defn ^:always-validate create-client-metric-registry :- ClientMetricRegistry
  [metric-registry :- MetricRegistry]
  (ClientMetricRegistry. metric-registry))
