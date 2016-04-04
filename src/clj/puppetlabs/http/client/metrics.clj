(ns puppetlabs.http.client.metrics
  (:require [schema.core :as schema]
            [puppetlabs.http.client.common :as common])
  (:import (com.codahale.metrics Timer)
           (java.util.concurrent TimeUnit)
           (com.puppetlabs.http.client.impl Metrics$MetricType Metrics)))

(schema/defn get-mean :- schema/Num
  [timer :- Timer]
  (->> timer
       .getSnapshot
       .getMean
       (.toMillis TimeUnit/NANOSECONDS)))

(defn get-metric-data
  [timer metric-id]
  (let [count (.getCount timer)
        mean (get-mean timer)
        aggregate (* count mean)]
    {:count count
     :mean mean
     :aggregate aggregate
     :metric-id metric-id}))

(defn get-metrics-data
  [timers]
  (reduce (fn [acc [metric-id timer]]
            (assoc acc metric-id (get-metric-data timer metric-id)))
          {} timers))

(defn get-java-metric-type
  [metric-type]
  (case metric-type
    :bytes-read Metrics$MetricType/BYTES_READ))

(schema/defn ^:always-validate get-client-metrics :- (schema/maybe common/Metrics)
  "Returns the http client-specific metrics from the metric registry."
  ([metric-registry :- common/OptionalMetricRegistry]
   (when metric-registry
     (into {} (Metrics/getClientMetrics metric-registry))))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-filter :- common/MetricFilter]
   (when metric-registry
     (cond
       (:method metric-filter) (into {} (Metrics/getClientMetricsWithUrlAndMethod
                                         metric-registry
                                         (:url metric-filter)
                                         (:method metric-filter)
                                         (get-java-metric-type (:metric-type metric-filter))))
       (:url metric-filter) (into {} (Metrics/getClientMetricsWithUrl
                                      metric-registry
                                      (:url metric-filter)
                                      (get-java-metric-type (:metric-type metric-filter))))
       (:metric-id metric-filter) (into {} (Metrics/getClientMetricsWithMetricId
                                            metric-registry
                                            (into-array (map name (:metric-id metric-filter)))
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
