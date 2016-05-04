package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Metric;
import com.puppetlabs.http.client.metrics.MetricIdClientTimer;
import com.puppetlabs.http.client.metrics.Metrics;
import com.puppetlabs.http.client.metrics.ClientTimer;
import com.puppetlabs.http.client.metrics.UrlAndMethodClientTimer;
import com.puppetlabs.http.client.metrics.UrlClientTimer;

import java.util.ArrayList;

public class ClientMetricFilter implements MetricFilter{
    private String category;
    private String url;
    private String method;
    private ArrayList<String> metricId;
    private Metrics.MetricType metricType;

    // TODO: break this class up into two or more filter classes; it's combining a lot of logic at the moment

    public ClientMetricFilter(String category, Metrics.MetricType metricType) {
        this.category = category;
        this.metricType = metricType;
    }

    public ClientMetricFilter(String url, String method, ArrayList<String> metricId,
                              Metrics.MetricType metricType) {
        this.category = null;
        this.url = url;
        this.method = method;
        this.metricId = metricId;
        this.metricType = metricType;
    }

    private boolean isMatch(ClientTimer metric) {
        if ( metric.getMetricType().equals(metricType) ) {
            if ( category != null ) {
                switch (category) {
                    // TODO: we should be able to break this up into multiple methods that accept the more
                    // concrete types in their signatures
                    case Metrics.ID_NAMESPACE:
                        return metric instanceof MetricIdClientTimer;
                    case Metrics.URL_METHOD_NAMESPACE:
                        return metric instanceof UrlAndMethodClientTimer;
                    case Metrics.URL_NAMESPACE:
                        return (metric instanceof UrlClientTimer) &&
                                !(metric instanceof UrlAndMethodClientTimer);
                }
            } else {
                if ( method != null ) {
                    // TODO: we should be able to break this up into multiple methods that accept the more
                    if (metric instanceof UrlAndMethodClientTimer) {
                        UrlAndMethodClientTimer urlAndMethodClientTimer = (UrlAndMethodClientTimer) metric;
                        return url.equals(urlAndMethodClientTimer.getUrl()) && method.equals(urlAndMethodClientTimer.getMethod());
                    } else {
                        return false;
                    }
                } else if ( url != null ) {
                    if ((metric instanceof UrlClientTimer) &&
                        !(metric instanceof UrlAndMethodClientTimer)) {
                        UrlClientTimer urlClientTimer = (UrlClientTimer) metric;
                        return url.equals(urlClientTimer.getUrl());
                    } else {
                        return false;
                    }
                } else {
                    if (metric instanceof MetricIdClientTimer) {
                        MetricIdClientTimer metricIdClientTimer = (MetricIdClientTimer) metric;
                        return metricId.equals(metricIdClientTimer.getMetricId());
                    } else {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public boolean matches(String s, Metric metric) {
        if ( metric instanceof ClientTimer ){
            return isMatch((ClientTimer) metric);
        } else {
            return false;
        }
    }
}
