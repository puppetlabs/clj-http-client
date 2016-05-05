package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.Metric;
import com.puppetlabs.http.client.metrics.UrlAndMethodClientTimer;

public class UrlAndMethodClientTimerFilter extends UrlClientTimerFilter {
    private final String method;

    public UrlAndMethodClientTimerFilter(String url, String method) {
        super(url);
        this.method = method;
    }

    @Override
    public boolean matches(String s, Metric metric) {
        if (metric.getClass().equals(UrlAndMethodClientTimer.class)) {
            UrlAndMethodClientTimer timer = (UrlAndMethodClientTimer) metric;
            return timer.getMethod().equals(this.method) &&
                    timer.getUrl().equals(this.getUrl());
        }
        return false;
    }
}
