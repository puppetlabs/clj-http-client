package com.puppetlabs.http.client.impl;

public class ClientMetricData {
    private String metricName;
    private Long count;
    private Long mean;
    private Long aggregate;

    ClientMetricData(String metricName, Long count, Long mean, Long aggregate) {
        this.metricName = metricName;
        this.count = count;
        this.mean = mean;
        this.aggregate = aggregate;
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
}

