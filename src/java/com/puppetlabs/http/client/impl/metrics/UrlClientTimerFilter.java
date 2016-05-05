package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.puppetlabs.http.client.metrics.UrlClientTimer;

public class UrlClientTimerFilter implements MetricFilter {
    private final String url;

    public UrlClientTimerFilter(String url) {
        this.url = url;
    }

    protected String getUrl() {
        return url;
    }

    @Override
    public boolean matches(String s, Metric metric) {
        return metric.getClass().equals(UrlClientTimer.class) &&
                ((UrlClientTimer) metric).
                        getUrl().equals(url);
    }
}
