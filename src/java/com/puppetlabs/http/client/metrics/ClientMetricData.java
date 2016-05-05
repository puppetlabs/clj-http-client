package com.puppetlabs.http.client.metrics;

import com.puppetlabs.http.client.impl.metrics.TimerMetricData;

public abstract class ClientMetricData {
    private final TimerMetricData timerMetricData;

    ClientMetricData(TimerMetricData timerMetricData) {
        this.timerMetricData = timerMetricData;
    }

    public String getMetricName() {
        return timerMetricData.getMetricName();
    }

    public Long getCount() {
        return timerMetricData.getCount();
    }

    public Long getMean() {
        return timerMetricData.getMeanMillis();
    }

    public Long getAggregate() {
        return timerMetricData.getAggregate();
    }
}

