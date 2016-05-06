package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.puppetlabs.http.client.metrics.ClientTimer;
import com.puppetlabs.http.client.metrics.Metrics;

public class CategoryClientTimerMetricFilter implements MetricFilter {
    private final Metrics.MetricCategory category;

    public CategoryClientTimerMetricFilter(Metrics.MetricCategory category) {
        this.category = category;
    }

    @Override
    public boolean matches(String s, Metric metric) {
        return metric instanceof ClientTimer &&
                ((ClientTimer) metric).isCategory(category);
    }
}
