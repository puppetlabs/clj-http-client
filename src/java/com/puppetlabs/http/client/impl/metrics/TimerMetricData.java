package com.puppetlabs.http.client.impl.metrics;

import com.puppetlabs.http.client.metrics.ClientTimer;

import java.util.concurrent.TimeUnit;

public class TimerMetricData {

    public static TimerMetricData fromTimer(ClientTimer timer) {
        Double mean = timer.getSnapshot().getMean();
        Long count = timer.getCount();
        Long meanMillis = TimeUnit.NANOSECONDS.toMillis(mean.longValue());

        return new TimerMetricData(
                timer.getMetricName(),
                meanMillis,
                count,
                count * meanMillis);
    }


    private final String metricName;
    private final Long meanMillis;
    private final Long count;
    private final Long aggregate;

    public TimerMetricData(String metricName, Long meanMillis,
                           Long count, Long aggregate) {
        this.metricName = metricName;
        this.meanMillis = meanMillis;
        this.count = count;
        this.aggregate = aggregate;
    }

    public String getMetricName() {
        return metricName;
    }

    public Long getMeanMillis() {
        return meanMillis;
    }

    public Long getCount() {
        return count;
    }

    public Long getAggregate() {
        return aggregate;
    }
}
