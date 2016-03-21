package com.puppetlabs.http.client.impl;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;
import org.apache.commons.lang3.StringUtils;

public class ClientMetricFilter {

    static class ClientFilter implements MetricFilter {
        public boolean matches (String name, Metric metric) {
            return name.startsWith(Metrics.METRIC_NAMESPACE);
        }
    }

    static class UrlFilter implements MetricFilter {
        private String url;
        private Metrics.MetricType metricType;

        public UrlFilter(String url, Metrics.MetricType metricType) {
            this.url = url;
            this.metricType = metricType;
        }

        public boolean matches(String s, Metric metric) {
            String[] metricNameArray = { Metrics.METRIC_NAMESPACE, Metrics.URL_NAMESPACE, url,
                    Metrics.metricTypeString(metricType) };
            return s.equals(StringUtils.join(metricNameArray, "."));
        }
    }

    static class UrlAndVerbFilter implements MetricFilter {
        private String url;
        private String verb;
        private Metrics.MetricType metricType;

        public UrlAndVerbFilter(String url, String verb, Metrics.MetricType metricType) {
            this.url = url;
            this.verb = verb;
            this.metricType = metricType;
        }

        public boolean matches(String s, Metric metric) {
            String[] metricNameArray = { Metrics.METRIC_NAMESPACE, Metrics.URL_NAMESPACE, url, verb,
                    Metrics.metricTypeString(metricType) };
            return s.equals(StringUtils.join(metricNameArray, "."));
        }
    }

    static class MetricIdFilter implements MetricFilter {
        private String[] metricId;
        private Metrics.MetricType metricType;

        public MetricIdFilter(String[] metricId, Metrics.MetricType metricType) {
            this.metricId = metricId;
            this.metricType = metricType;
        }

        public boolean matches(String s, Metric metric) {
            String[] metricNameArray = { Metrics.METRIC_NAMESPACE, Metrics.ID_NAMESPACE,
                    StringUtils.join(metricId, "."), Metrics.metricTypeString(metricType) };
            return s.equals(StringUtils.join(metricNameArray, "."));
        }
    }
}
