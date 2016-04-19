package com.puppetlabs.http.client.impl;

import com.codahale.metrics.Timer;

import java.util.ArrayList;

public class ClientTimer  extends Timer {
    private String metricName;
    private String url;
    private String method;
    private ArrayList<String> metricId;

    private Metrics.MetricType metricType;

    public ClientTimer(String metricName, String url, Metrics.MetricType metricType) {
        super();
        this.metricName = metricName;
        this.url = url;
        this.metricType = metricType;
    }

    public ClientTimer(String metricName, String url, String method,
                       Metrics.MetricType metricType) {
        super();
        this.metricName = metricName;
        this.url = url;
        this.method = method;
        this.metricType = metricType;
    }

    public ClientTimer(String metricName, ArrayList<String> metricId,
                       Metrics.MetricType metricType) {
        super();
        this.metricName = metricName;
        this.metricId = metricId;
        this.metricType = metricType;
    }

    public String getMetricName() {
        return metricName;
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

    public Metrics.MetricType getMetricType() {
        return metricType;
    }
}
