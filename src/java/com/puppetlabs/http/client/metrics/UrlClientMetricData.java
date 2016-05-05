package com.puppetlabs.http.client.metrics;

import com.puppetlabs.http.client.impl.metrics.TimerMetricData;

public class UrlClientMetricData extends ClientMetricData {
    private final String url;

    public UrlClientMetricData(TimerMetricData timerMetricData,
                               String url) {
        super(timerMetricData);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
