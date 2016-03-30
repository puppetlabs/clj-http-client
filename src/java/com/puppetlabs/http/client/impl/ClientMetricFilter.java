package com.puppetlabs.http.client.impl;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;

public class ClientMetricFilter implements MetricFilter{
    private String name;

    public ClientMetricFilter() {
        this.name = null;
    }

    public ClientMetricFilter(String name) {
        this.name = name;
    }

    public boolean matches(String s, Metric metric) {
        if ( name == null ) {
            return s.startsWith(Metrics.METRIC_NAMESPACE);
        } else {
            return s.equals(name);
        }
    }
}
