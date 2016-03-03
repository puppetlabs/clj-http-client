package com.puppetlabs.http.client.impl;

public class ClientMetricData {
    private String metricId;
    private Long count;
    private Long mean;
    private Long aggregate;

    ClientMetricData(String metricId, Long count, Long mean, Long aggregate) {
        this.metricId = metricId;
        this.count = count;
        this.mean = mean;
        this.aggregate = aggregate;
    }

    public String getMetricId() {
        return metricId;
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

