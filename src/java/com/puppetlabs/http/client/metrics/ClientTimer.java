package com.puppetlabs.http.client.metrics;

import com.codahale.metrics.Timer;

public abstract class ClientTimer  extends Timer {
    private final String metricName;

    private final Metrics.MetricType metricType;

    ClientTimer(String metricName, Metrics.MetricType metricType) {
        super();
        this.metricName = metricName;
        this.metricType = metricType;
    }

    public String getMetricName() {
        return metricName;
    }

    public Metrics.MetricType getMetricType() {
        return metricType;
    }

    public abstract boolean isCategory(String category);
}
