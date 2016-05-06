package com.puppetlabs.http.client.metrics;

import com.puppetlabs.http.client.impl.metrics.TimerMetricData;

import java.util.List;

public class MetricIdClientMetricData extends ClientMetricData {
    private final List<String> metricId;

    public MetricIdClientMetricData(TimerMetricData timerMetricData,
                                    List<String> metricId) {
        super(timerMetricData);
        this.metricId = metricId;
    }

    public List<String> getMetricId() {
        return metricId;
    }
}
