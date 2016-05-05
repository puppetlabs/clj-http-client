package com.puppetlabs.http.client.metrics;

import java.util.List;

public class ClientMetricDataContainer {
    private final List<UrlClientMetricData> urlData;
    private final List<UrlAndMethodClientMetricData> urlAndMethodData;
    private final List<MetricIdClientMetricData> metricIdData;

    public ClientMetricDataContainer(List<UrlClientMetricData> urlTimers,
                                     List<UrlAndMethodClientMetricData> urlAndMethodData,
                                     List<MetricIdClientMetricData> metricIdData) {
        this.urlData = urlTimers;
        this.urlAndMethodData = urlAndMethodData;
        this.metricIdData = metricIdData;
    }

    public List<UrlClientMetricData> getUrlData() {
        return urlData;
    }

    public List<UrlAndMethodClientMetricData> getUrlAndMethodData() {
        return urlAndMethodData;
    }

    public List<MetricIdClientMetricData> getMetricIdData() {
        return metricIdData;
    }
}
