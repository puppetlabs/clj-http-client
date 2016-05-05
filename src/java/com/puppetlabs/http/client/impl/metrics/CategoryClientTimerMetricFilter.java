package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.puppetlabs.http.client.metrics.*;

public class CategoryClientTimerMetricFilter implements MetricFilter {
    private final String category;

    public CategoryClientTimerMetricFilter(String category) {
        this.category = category;
    }

    @Override
    public boolean matches(String s, Metric metric) {
        if (metric instanceof ClientTimer) {
            return ((ClientTimer)metric).isCategory(category);
        } else {
            return false;
        }
    }
}
