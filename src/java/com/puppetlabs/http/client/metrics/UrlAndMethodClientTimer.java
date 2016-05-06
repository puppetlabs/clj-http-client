package com.puppetlabs.http.client.metrics;

public class UrlAndMethodClientTimer extends UrlClientTimer {
    private final String method;

    public UrlAndMethodClientTimer(String metricName, String url, String method,
                                   Metrics.MetricType metricType) {
        super(metricName, url, metricType);
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean isCategory(Metrics.MetricCategory category) {
        return category.equals(Metrics.MetricCategory.URL_AND_METHOD);
    }
}
