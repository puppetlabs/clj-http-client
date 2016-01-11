package com.puppetlabs.http.client.impl;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;

public class ClientMetricFilter implements MetricFilter{

    public boolean matches (String name, Metric metric) {
        return name.startsWith("puppetlabs.http-client");
    }
}
