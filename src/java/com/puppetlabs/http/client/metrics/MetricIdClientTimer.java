package com.puppetlabs.http.client.metrics;

import java.util.ArrayList;
import java.util.List;

public class MetricIdClientTimer extends ClientTimer {

    private final List<String> metricId;

    public MetricIdClientTimer(String metricName, List<String> metricId,
                       Metrics.MetricType metricType) {
        super(metricName, metricType);
        this.metricId = metricId;
    }

    public List<String> getMetricId() {
        return metricId;
    }
}
