package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.puppetlabs.http.client.metrics.MetricIdClientTimer;

import java.util.List;

public class MetricIdClientTimerFilter implements MetricFilter {
    private final List<String> metricId;

    public MetricIdClientTimerFilter(List<String> metricId) {
        this.metricId = metricId;
    }

    @Override
    public boolean matches(String s, Metric metric) {
        return metric.getClass().equals(MetricIdClientTimer.class) &&
                ((MetricIdClientTimer) metric).
                        getMetricId().equals(metricId);
    }
}
