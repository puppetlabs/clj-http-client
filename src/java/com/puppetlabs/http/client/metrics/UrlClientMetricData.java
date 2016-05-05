package com.puppetlabs.http.client.metrics;

public class UrlClientMetricData extends ClientMetricData {
    private final String url;

    public UrlClientMetricData(String metricName, Long count, Long mean, Long aggregate,
                               String url) {
        super(metricName, count, mean, aggregate);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
