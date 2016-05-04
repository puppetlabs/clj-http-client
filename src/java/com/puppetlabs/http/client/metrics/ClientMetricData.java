package com.puppetlabs.http.client.metrics;

import java.util.List;

public class ClientMetricData {
    private String metricName;
    private Long count;
    private Long mean;
    private Long aggregate;
    private String url;
    private String method;
    private List<String> metricId;

    ClientMetricData(String metricName, Long count, Long mean, Long aggregate,
                     String url, String method, List<String> metricId) {
        this.metricName = metricName;
        this.count = count;
        this.mean = mean;
        this.aggregate = aggregate;
        this.url = url;
        this.method = method;
        this.metricId = metricId;
    }

    public String getMetricName() {
        return metricName;
    }

    public Long getCount() {
        return count;
    }

    public Long getMean() {
        return mean;
    }

    public Long getAggregate() {
        return aggregate;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getMetricId() {
        return metricId;
    }
}

