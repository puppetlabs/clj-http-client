package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;
import com.puppetlabs.http.client.metrics.MetricIdClientTimer;
import com.puppetlabs.http.client.metrics.Metrics;
import com.puppetlabs.http.client.metrics.UrlAndMethodClientTimer;
import com.puppetlabs.http.client.metrics.UrlClientTimer;

import java.util.ArrayList;

public class ClientMetricFilter implements MetricFilter{
    private String url;
    private String method;
    private ArrayList<String> metricId;
    private Metrics.MetricType metricType;

    public ClientMetricFilter(String url, String method, ArrayList<String> metricId,
                              Metrics.MetricType metricType) {
        this.url = url;
        this.method = method;
        this.metricId = metricId;
        this.metricType = metricType;
    }

    private boolean isMatch(UrlClientTimer metric) {
        if ( metric.getMetricType().equals(metricType) ) {
            if ( method != null ) {
                return false;
            } else if ( url != null ) {
                return url.equals(metric.getUrl());
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isMatch(UrlAndMethodClientTimer metric) {
        if ( metric.getMetricType().equals(metricType) ) {
            if ( method != null ) {
                return url.equals(metric.getUrl()) && method.equals(metric.getMethod());
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean isMatch(MetricIdClientTimer metric) {
        if ( metric.getMetricType().equals(metricType) ) {
            if ( method != null ) {
                return false;
            } else if ( url != null ) {
                return false;
            } else {
                return metricId.equals(metric.getMetricId());
            }
        }
        return false;
    }

    public boolean matches(String s, Metric metric) {
        if (metric instanceof UrlAndMethodClientTimer) {
            return isMatch((UrlAndMethodClientTimer) metric);
        } else if (metric instanceof UrlClientTimer) {
            return isMatch((UrlClientTimer) metric);
        } else if (metric instanceof MetricIdClientTimer) {
            return isMatch((MetricIdClientTimer) metric);
        } else {
            return false;
        }
    }
}
