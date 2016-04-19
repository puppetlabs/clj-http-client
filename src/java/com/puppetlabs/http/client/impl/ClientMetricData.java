package com.puppetlabs.http.client.impl;

import java.util.ArrayList;

public class ClientMetricData {
    private String metricName;
    private Long count;
    private Long mean;
    private Long aggregate;
    private String url;
    private String method;
    private ArrayList<String> metricId;

    ClientMetricData(String metricName, Long count, Long mean, Long aggregate,
                     String url, String method, ArrayList<String> metricId) {
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

    public ArrayList<String> getMetricId() {
        return metricId;
    }
}

