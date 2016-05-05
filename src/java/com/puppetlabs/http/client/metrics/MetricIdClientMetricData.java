package com.puppetlabs.http.client.metrics;

import java.util.List;

public class MetricIdClientMetricData extends ClientMetricData {
    private final List<String> metricId;

    public MetricIdClientMetricData(String metricName, Long count, Long mean, Long aggregate,
                                    List<String> metricId) {
        super(metricName, count, mean, aggregate);
        this.metricId = metricId;
    }

    public List<String> getMetricId() {
        return metricId;
    }
}
