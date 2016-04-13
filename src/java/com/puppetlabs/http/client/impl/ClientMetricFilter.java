package com.puppetlabs.http.client.impl;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;

public class ClientMetricFilter implements MetricFilter{
    private String category;
    private String name;

    public ClientMetricFilter(String category) {
        this.category = category;
    }

    public ClientMetricFilter(String category, String name) {
        this.category = null; // HACK for the moment
        this.name = name;
    }

    public boolean matches(String s, Metric metric) {
        if ( category != null) {
            return s.startsWith(Metrics.METRIC_NAMESPACE + "." + category);
        } else {
            return s.equals(name);
        }
    }
}
