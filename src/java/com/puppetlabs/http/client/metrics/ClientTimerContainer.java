package com.puppetlabs.http.client.metrics;

import java.util.List;

public class ClientTimerContainer {
    private final List<UrlClientTimer> urlTimers;
    private final List<UrlAndMethodClientTimer> urlAndMethodTimers;
    private final List<MetricIdClientTimer> metricIdTimers;

    public ClientTimerContainer(List<UrlClientTimer> urlTimers,
                                List<UrlAndMethodClientTimer> urlAndMethodTimers,
                                List<MetricIdClientTimer> metricIdTimers) {
        this.urlTimers = urlTimers;
        this.urlAndMethodTimers = urlAndMethodTimers;
        this.metricIdTimers = metricIdTimers;
    }

    public List<UrlClientTimer> getUrlTimers() {
        return urlTimers;
    }

    public List<UrlAndMethodClientTimer> getUrlAndMethodTimers() {
        return urlAndMethodTimers;
    }

    public List<MetricIdClientTimer> getMetricIdTimers() {
        return metricIdTimers;
    }
}
