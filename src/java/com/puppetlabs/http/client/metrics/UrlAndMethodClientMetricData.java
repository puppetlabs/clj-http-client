package com.puppetlabs.http.client.metrics;

import com.puppetlabs.http.client.impl.metrics.TimerMetricData;

public class UrlAndMethodClientMetricData extends UrlClientMetricData {
    private final String method;

    public UrlAndMethodClientMetricData(TimerMetricData timerMetricData,
                                        String url, String method) {
        super(timerMetricData, url);
        this.method = method;
    }

    public String getMethod() {
        return method;
    }
}
