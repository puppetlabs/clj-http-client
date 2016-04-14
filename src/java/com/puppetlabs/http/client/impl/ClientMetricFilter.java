package com.puppetlabs.http.client.impl;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;

import java.util.ArrayList;

public class ClientMetricFilter implements MetricFilter{
    private String category;
    private String url;
    private String method;
    private ArrayList<String> metricId;
    private Metrics.MetricType metricType;

    public ClientMetricFilter(String category, Metrics.MetricType metricType) {
        this.category = category;
        this.metricType = metricType;
    }

    public ClientMetricFilter(String url, String method, ArrayList<String> metricId, Metrics.MetricType metricType) {
        this.category = null;
        this.url = url;
        this.method = method;
        this.metricId = metricId;
        this.metricType = metricType;
    }

    private boolean isMatch(ClientTimer metric, String url, String method, ArrayList<String> metricId, Metrics.MetricType metricType) {
        if ( metric.getMetricType().equals(metricType) ) {
            if ( category != null ) {
                switch (category) {
                    case Metrics.ID_NAMESPACE:
                        return metric.getMetricId() != null;
                    case Metrics.URL_METHOD_NAMESPACE:
                        return  metric.getMethod() != null;
                    case Metrics.URL_NAMESPACE:
                        return metric.getUrl() != null && metric.getMethod() == null;
                }
            } else {
                if ( method != null ) {
                    return url.equals(metric.getUrl()) && method.equals(metric.getMethod()) ;
                } else if ( url != null ) {
                    return url.equals(metric.getUrl()) && metric.getMethod() == null;
                } else {
                    return metricId.equals(metric.getMetricId());
                }
            }
        }
        return false;
    }

    public boolean matches(String s, Metric metric) {
        if ( metric instanceof ClientTimer ){
            return isMatch((ClientTimer) metric, url, method, metricId, metricType);
        } else {
            return false;
        }
    }
}
