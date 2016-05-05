package com.puppetlabs.http.client.metrics;

public class UrlAndMethodClientMetricData extends UrlClientMetricData {
    private final String method;

    public UrlAndMethodClientMetricData(String metricName, Long count, Long mean, Long aggregate,
                                        String url, String method) {
        super(metricName, count, mean, aggregate, url);
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
