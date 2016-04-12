package com.puppetlabs.http.client.impl;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Metrics {
    public static final String METRIC_NAMESPACE = "puppetlabs.http-client.experimental";
    public static final String URL_NAMESPACE = "with-url";
    public static final String URL_METHOD_NAMESPACE = "with-url-and-method";
    public static final String ID_NAMESPACE = "with-metric-id";
    public static final String BYTES_READ_STRING = "bytes-read";
    public enum MetricType { BYTES_READ; }

    public static String metricTypeString(Metrics.MetricType metricType) {
        // currently this is the only metric type we have; in the future when
        // there are multiple types this will do something more useful
        return BYTES_READ_STRING;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);

    private static ArrayList<Timer.Context> startBytesReadMetricIdTimers(ClientMetricRegistry registry,
                                                                         String[] metricId) {
        ArrayList<Timer.Context> timers = new ArrayList<>();
        ConcurrentHashMap<ArrayList<String>, String> metricIdNames = registry.getMetricIdMetricNames();
        for (int i = 0; i < metricId.length; i++) {
            ArrayList<String> currentId = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                currentId.add(metricId[j]);
            }
            ArrayList<String> currentIdWithNamespace = new ArrayList<>();
            currentIdWithNamespace.add(ID_NAMESPACE);
            currentIdWithNamespace.addAll(currentId);
            currentIdWithNamespace.add(BYTES_READ_STRING);
            String metric_name = MetricRegistry.name(METRIC_NAMESPACE,
                currentIdWithNamespace.toArray(new String[currentIdWithNamespace.size()]));
            timers.add(registry.timer(metric_name).time());
            metricIdNames.putIfAbsent(currentId, metric_name);
        }
        return timers;
    }

    private static ArrayList<Timer.Context> startBytesReadUrlTimers(ClientMetricRegistry registry,
                                                                    HttpRequest request) {
        ArrayList<Timer.Context> timers = new ArrayList<>();
        try {
            ConcurrentHashMap<String, String> urlMetricNames = registry.getUrlMetricNames();
            ConcurrentHashMap<String, String> urlMethodMetricNames = registry.getUrlMethodMetricNames();
            final RequestLine requestLine = request.getRequestLine();
            final URI uri = new URI(requestLine.getUri());

             // if the port is not specified, `getPort()` returns -1
            final String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            final String strippedUrl = uri.getScheme() + "://" + uri.getHost()
                    + port + uri.getRawPath();
            final String method = requestLine.getMethod();

            final String urlName = MetricRegistry.name(METRIC_NAMESPACE, URL_NAMESPACE,
                    strippedUrl, BYTES_READ_STRING);
            final String urlAndMethodName = MetricRegistry.name(METRIC_NAMESPACE, URL_METHOD_NAMESPACE,
                    strippedUrl, method, BYTES_READ_STRING);

            urlMetricNames.putIfAbsent(strippedUrl, urlName);
            timers.add(registry.timer(urlName).time());

            urlMethodMetricNames.putIfAbsent(strippedUrl + "." + method, urlAndMethodName);
            timers.add(registry.timer(urlAndMethodName).time());
        } catch (URISyntaxException e) {
            // this shouldn't be possible
            LOGGER.warn("Could not build URI out of the request URI. Will not create URI timers. " +
                    "We recommend you read http://www.stilldrinking.com/programming-sucks. " +
                    "'now all your snowflakes are urine and you can't even find the cat.'");
        }
        return timers;
    }

    public static ArrayList<Timer.Context> startBytesReadTimers(ClientMetricRegistry clientRegistry,
                                                                HttpRequest request,
                                                                String[] metricId) {
        if (clientRegistry != null) {
            ArrayList<Timer.Context> urlTimers = startBytesReadUrlTimers(clientRegistry, request);
            ArrayList<Timer.Context> allTimers = new ArrayList<>(urlTimers);
            if (metricId != null) {
                ArrayList<Timer.Context> metricIdTimers =
                        startBytesReadMetricIdTimers(clientRegistry, metricId);
                allTimers.addAll(metricIdTimers);
            }
            return allTimers;
        }
        else {
            return null;
        }
    }

    public static Map<String, Timer> getClientMetrics(ClientMetricRegistry metricRegistry){
        if (metricRegistry != null) {
            return metricRegistry.getTimers(new ClientMetricFilter("all"));
        } else {
            return null;
        }
    }

    public static Map<String, Timer> getClientMetrics(ClientMetricRegistry metricRegistry,
                                                      final String url,
                                                      final MetricType metricType){
        if (metricRegistry != null) {
            String metricName = metricRegistry.getUrlMetricNames().get(url);
            return metricRegistry.getTimers(new ClientMetricFilter(metricName));
        } else {
            return null;
        }
    }

    public static Map<String, Timer> getClientMetrics(ClientMetricRegistry metricRegistry,
                                                      final String url,
                                                      final String method,
                                                      final MetricType metricType){
        if (metricRegistry != null) {
            String metricName = metricRegistry.getUrlMethodMetricNames().get(url + "." + method);
            return metricRegistry.getTimers(new ClientMetricFilter(metricName));
        } else {
            return null;
        }
    }

    public static Map<String, Timer> getClientMetrics(ClientMetricRegistry metricRegistry,
                                                      final String[] metricId,
                                                      final MetricType metricType){
        if (metricRegistry != null) {
            if (metricId.length == 0) {
                String metricNameStart = MetricRegistry.name(METRIC_NAMESPACE, ID_NAMESPACE);
                String metricNameEnd = metricTypeString(metricType);
                return metricRegistry.getTimers(new ClientMetricFilter(metricNameStart, metricNameEnd));
            } else {
                String metricName = metricRegistry.getMetricIdMetricNames().get(new ArrayList<String>(Arrays.asList(metricId)));
                return metricRegistry.getTimers(new ClientMetricFilter(metricName));
            }
        } else {
            return null;
        }
    }

    public static Map<String, ClientMetricData> computeClientMetricsData(Map<String, Timer> timers){
        if (timers != null) {
            Map<String, ClientMetricData> metricsData = new HashMap<>();
            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                Timer timer = entry.getValue();
                String metricName = entry.getKey();
                Double mean = timer.getSnapshot().getMean();
                Long meanMillis = TimeUnit.NANOSECONDS.toMillis(mean.longValue());
                Long count = timer.getCount();
                Long aggregate = count * meanMillis;

                ClientMetricData data = new ClientMetricData(metricName, count, meanMillis, aggregate);
                metricsData.put(metricName, data);
            }
            return metricsData;
        } else {
            return null;
        }
    }

    public static Map<String, ClientMetricData> getClientMetricsData(ClientMetricRegistry metricRegistry){
        Map<String, Timer> timers = getClientMetrics(metricRegistry);
        return computeClientMetricsData(timers);
    }

    public static Map<String, ClientMetricData> getClientMetricsData(ClientMetricRegistry metricRegistry,
                                                                     String url,
                                                                     MetricType metricType){
        Map<String, Timer> timers = getClientMetrics(metricRegistry, url, metricType);
        return computeClientMetricsData(timers);
    }

    public static Map<String, ClientMetricData> getClientMetricsData(ClientMetricRegistry metricRegistry,
                                                                     String url,
                                                                     String method,
                                                                     MetricType metricType){
        Map<String, Timer> timers = getClientMetrics(metricRegistry, url, method, metricType);
        return computeClientMetricsData(timers);
    }

    public static Map<String, ClientMetricData> getClientMetricsData(ClientMetricRegistry metricRegistry,
                                                                     String[] metricId,
                                                                     MetricType metricType){
        Map<String, Timer> timers = getClientMetrics(metricRegistry, metricId, metricType);
        return computeClientMetricsData(timers);
    }
}
