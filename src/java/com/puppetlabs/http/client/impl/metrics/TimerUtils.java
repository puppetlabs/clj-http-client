package com.puppetlabs.http.client.impl.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.puppetlabs.http.client.metrics.ClientTimer;
import com.puppetlabs.http.client.metrics.MetricIdClientTimer;
import com.puppetlabs.http.client.metrics.Metrics;
import com.puppetlabs.http.client.metrics.UrlAndMethodClientTimer;
import com.puppetlabs.http.client.metrics.UrlClientTimer;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

public class TimerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerUtils.class);

    private static ClientTimer getOrAddTimer(MetricRegistry metricRegistry,
                                             String name,
                                             ClientTimer newTimer) {
        final Map<String, Metric> metrics = metricRegistry.getMetrics();
        final Metric metric = metrics.get(name);
        if ( metric instanceof ClientTimer ) {
            return (ClientTimer) metric;
        } else if ( metric == null ) {
            try {
                return metricRegistry.register(name, newTimer);
            } catch (IllegalArgumentException e) {
                final Metric added = metricRegistry.getMetrics().get(name);
                if ( added instanceof ClientTimer ) {
                    return (ClientTimer) added;
                }
            }
        }
        throw new IllegalArgumentException(name +" is already used for a different type of metric");
    }

    private static ArrayList<Timer.Context> startFullResponseMetricIdTimers(MetricRegistry registry,
                                                                            String[] metricId,
                                                                            String metricPrefix) {
        ArrayList<Timer.Context> timerContexts = new ArrayList<>();
        for (int i = 0; i < metricId.length; i++) {
            ArrayList<String> currentId = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                currentId.add(metricId[j]);
            }
            ArrayList<String> currentIdWithNamespace = new ArrayList<>();
            currentIdWithNamespace.add(Metrics.NAMESPACE_METRIC_ID);
            currentIdWithNamespace.addAll(currentId);
            currentIdWithNamespace.add(Metrics.NAMESPACE_FULL_RESPONSE);
            String metric_name = MetricRegistry.name(metricPrefix,
                    currentIdWithNamespace.toArray(new String[currentIdWithNamespace.size()]));

            ClientTimer timer = new MetricIdClientTimer(metric_name, currentId, Metrics.MetricType.FULL_RESPONSE);
            timerContexts.add(getOrAddTimer(registry, metric_name, timer).time());
        }
        return timerContexts;
    }

    private static ArrayList<Timer.Context> startFullResponseUrlTimers(MetricRegistry registry,
                                                                       HttpRequest request,
                                                                       String metricPrefix,
                                                                       Boolean useUrlMetrics) {
        ArrayList<Timer.Context> timerContexts = new ArrayList<>();
        if (useUrlMetrics) {
            try {
                final RequestLine requestLine = request.getRequestLine();
                final String strippedUrl = Metrics.urlToMetricUrl(requestLine.getUri());
                final String method = requestLine.getMethod();

                final String urlName = MetricRegistry.name(metricPrefix, Metrics.NAMESPACE_URL,
                        strippedUrl, Metrics.NAMESPACE_FULL_RESPONSE);
                final String urlAndMethodName = MetricRegistry.name(metricPrefix, Metrics.NAMESPACE_URL_AND_METHOD,
                        strippedUrl, method, Metrics.NAMESPACE_FULL_RESPONSE);

                ClientTimer urlTimer = new UrlClientTimer(urlName, strippedUrl, Metrics.MetricType.FULL_RESPONSE);
                timerContexts.add(getOrAddTimer(registry, urlName, urlTimer).time());

                ClientTimer urlMethodTimer = new UrlAndMethodClientTimer(urlAndMethodName, strippedUrl,
                        method, Metrics.MetricType.FULL_RESPONSE);
                timerContexts.add(getOrAddTimer(registry, urlAndMethodName, urlMethodTimer).time());
            } catch (URISyntaxException e) {
                // this shouldn't be possible
                LOGGER.warn("Could not build URI out of the request URI. Will not create URI timers. " +
                        "We recommend you read http://www.stilldrinking.com/programming-sucks. " +
                        "'now all your snowflakes are urine and you can't even find the cat.'");
            }
        }
        return timerContexts;
    }

    public static ArrayList<Timer.Context> startFullResponseTimers(MetricRegistry clientRegistry,
                                                                   HttpRequest request,
                                                                   String[] metricId,
                                                                   String metricNamespace,
                                                                   Boolean useUrlMetrics) {
        if (clientRegistry != null) {
            ArrayList<Timer.Context> urlTimerContexts = startFullResponseUrlTimers(clientRegistry, request, metricNamespace, useUrlMetrics);
            ArrayList<Timer.Context> allTimerContexts = new ArrayList<>(urlTimerContexts);
            if (metricId != null) {
                ArrayList<Timer.Context> metricIdTimers =
                        startFullResponseMetricIdTimers(clientRegistry, metricId, metricNamespace);
                allTimerContexts.addAll(metricIdTimers);
            }
            return allTimerContexts;
        }
        else {
            return null;
        }
    }
}
