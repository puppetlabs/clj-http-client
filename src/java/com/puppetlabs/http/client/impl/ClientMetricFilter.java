package com.puppetlabs.http.client.impl;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;

public class ClientMetricFilter implements MetricFilter{
    private String name;
    private String nameEnd;

    public ClientMetricFilter() {
        this.name = null;
    }

    public ClientMetricFilter(String name) {
        this.name = name;
    }

    public ClientMetricFilter(String name, String nameEnd) {
        this.name = name;
        this.nameEnd = nameEnd;
    }

    public boolean matches(String s, Metric metric) {
        if ( name == "all" ) {
            return s.startsWith(Metrics.METRIC_NAMESPACE);
        } else if ( nameEnd == null ){
            return s.equals(name);
        } else {
            return s.startsWith(name) && s.endsWith(nameEnd);
        }
    }
}
