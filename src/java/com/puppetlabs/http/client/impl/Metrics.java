package com.puppetlabs.http.client.impl;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Metrics {
    public static final String METRIC_NAMESPACE = "puppetlabs.http-client.experimental";
    public static final String URL_NAMESPACE = "with-url";
    public static final String URL_METHOD_NAMESPACE = "with-url-and-method";
    public static final String ID_NAMESPACE = "with-metric-id";
    public static final String FULL_RESPONSE_STRING = "full-response";
    public enum MetricType { FULL_RESPONSE; }

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrics.class);

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
                                                                            String[] metricId) {
        ArrayList<Timer.Context> timerContexts = new ArrayList<>();
        for (int i = 0; i < metricId.length; i++) {
            ArrayList<String> currentId = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                currentId.add(metricId[j]);
            }
            ArrayList<String> currentIdWithNamespace = new ArrayList<>();
            currentIdWithNamespace.add(ID_NAMESPACE);
            currentIdWithNamespace.addAll(currentId);
            currentIdWithNamespace.add(FULL_RESPONSE_STRING);
            String metric_name = MetricRegistry.name(METRIC_NAMESPACE,
                    currentIdWithNamespace.toArray(new String[currentIdWithNamespace.size()]));

            ClientTimer timer = new ClientTimer(metric_name, currentId, MetricType.FULL_RESPONSE);
            timerContexts.add(getOrAddTimer(registry, metric_name, timer).time());
        }
        return timerContexts;
    }

    private static ArrayList<Timer.Context> startFullResponseUrlTimers(MetricRegistry registry,
                                                                       HttpRequest request) {
        ArrayList<Timer.Context> timerContexts = new ArrayList<>();
        try {
            final RequestLine requestLine = request.getRequestLine();
            final URI uri = new URI(requestLine.getUri());

            // if the port is not specified, `getPort()` returns -1
            final String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
            final String strippedUrl = uri.getScheme() + "://" + uri.getHost()
                    + port + uri.getRawPath();
            final String method = requestLine.getMethod();

            final String urlName = MetricRegistry.name(METRIC_NAMESPACE, URL_NAMESPACE,
                    strippedUrl, FULL_RESPONSE_STRING);
            final String urlAndMethodName = MetricRegistry.name(METRIC_NAMESPACE, URL_METHOD_NAMESPACE,
                    strippedUrl, method, FULL_RESPONSE_STRING);

            ClientTimer urlTimer = new ClientTimer(urlName, strippedUrl, MetricType.FULL_RESPONSE);
            timerContexts.add(getOrAddTimer(registry, urlName, urlTimer).time());

            ClientTimer urlMethodTimer = new ClientTimer(urlAndMethodName, strippedUrl,
                    method, MetricType.FULL_RESPONSE);
            timerContexts.add(getOrAddTimer(registry, urlAndMethodName, urlMethodTimer).time());
        } catch (URISyntaxException e) {
            // this shouldn't be possible
            LOGGER.warn("Could not build URI out of the request URI. Will not create URI timers. " +
                    "We recommend you read http://www.stilldrinking.com/programming-sucks. " +
                    "'now all your snowflakes are urine and you can't even find the cat.'");
        }
        return timerContexts;
    }

    public static ArrayList<Timer.Context> startFullResponseTimers(MetricRegistry clientRegistry,
                                                                   HttpRequest request,
                                                                   String[] metricId) {
        if (clientRegistry != null) {
            ArrayList<Timer.Context> urlTimerContexts = startFullResponseUrlTimers(clientRegistry,request);
            ArrayList<Timer.Context> allTimerContexts = new ArrayList<>(urlTimerContexts);
            if (metricId != null) {
                ArrayList<Timer.Context> metricIdTimers =
                        startFullResponseMetricIdTimers(clientRegistry, metricId);
                allTimerContexts.addAll(metricIdTimers);
            }
            return allTimerContexts;
        }
        else {
            return null;
        }
    }

    public static ArrayList<ClientTimer> getClientTimerArray(Map<String, Timer> timers){
        ArrayList<ClientTimer> timerArray = new ArrayList<>();
        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            ClientTimer timer = (ClientTimer)entry.getValue();
            timerArray.add(timer);
        }
        return timerArray;
    }

    public static Map<String, ArrayList<ClientTimer>> getClientMetrics(MetricRegistry metricRegistry){
        if (metricRegistry != null) {
            Map<String, ArrayList<ClientTimer>> timers = new HashMap<>();
            timers.put("url", getClientTimerArray(metricRegistry.getTimers(
                    new ClientMetricFilter(URL_NAMESPACE, MetricType.FULL_RESPONSE))));
            timers.put("url-and-method", getClientTimerArray(metricRegistry.getTimers(
                    new ClientMetricFilter(URL_METHOD_NAMESPACE, MetricType.FULL_RESPONSE))));
            timers.put("metric-id", getClientTimerArray(metricRegistry.getTimers(
                    new ClientMetricFilter(ID_NAMESPACE, MetricType.FULL_RESPONSE))));
            return timers;
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static ArrayList<ClientTimer> getClientMetricsByUrl(MetricRegistry metricRegistry,
                                                               final String url){
        if (metricRegistry != null) {
            Map<String, Timer> timers = metricRegistry.getTimers(
                    new ClientMetricFilter(url, null, null, MetricType.FULL_RESPONSE));
            return getClientTimerArray(timers);
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static ArrayList<ClientTimer> getClientMetricsByUrlAndMethod(MetricRegistry metricRegistry,
                                                                        final String url,
                                                                        final String method){
        if (metricRegistry != null) {
            Map<String, Timer> timers = metricRegistry.getTimers(
                    new ClientMetricFilter(url, method, null, MetricType.FULL_RESPONSE));
            return getClientTimerArray(timers);
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static ArrayList<ClientTimer> getClientMetricsByMetricId(MetricRegistry metricRegistry,
                                                                    final String[] metricId){
        if (metricRegistry != null) {
            if (metricId.length == 0) {
                Map<String, Timer> timers = metricRegistry.getTimers(
                        new ClientMetricFilter(ID_NAMESPACE, MetricType.FULL_RESPONSE));
                return getClientTimerArray(timers);
            } else {
                Map<String, Timer> timers = metricRegistry.getTimers(
                        new ClientMetricFilter(null, null,
                                new ArrayList<String>(Arrays.asList(metricId)),
                                MetricType.FULL_RESPONSE));
                return getClientTimerArray(timers);
            }
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static ArrayList<ClientMetricData> computeClientMetricsData(ArrayList<ClientTimer> timers){
        if (timers != null) {
            ArrayList<ClientMetricData> metricsData = new ArrayList<>();
            for (ClientTimer timer: timers) {
                Double mean = timer.getSnapshot().getMean();
                Long meanMillis = TimeUnit.NANOSECONDS.toMillis(mean.longValue());
                Long count = timer.getCount();
                Long aggregate = count * meanMillis;
                String metricName = timer.getMetricName();
                String url = timer.getUrl();
                String method = timer.getMethod();
                ArrayList<String> metricId = timer.getMetricId();

                ClientMetricData data = new ClientMetricData(metricName, count, meanMillis,
                        aggregate, url, method, metricId);
                metricsData.add(data);
            }
            return metricsData;
        } else {
            return null;
        }
    }

    public static Map<String, ArrayList<ClientMetricData>> getClientMetricsData(MetricRegistry metricRegistry){
        if ( metricRegistry != null ) {
            Map<String, ArrayList<ClientTimer>> timers = getClientMetrics(metricRegistry);
            Map<String, ArrayList<ClientMetricData>> data = new HashMap<>();
            data.put("url", computeClientMetricsData(timers.get("url")));
            data.put("url-and-method", computeClientMetricsData(timers.get("url-and-method")));
            data.put("metric-id", computeClientMetricsData(timers.get("metric-id")));
            return data;
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static ArrayList<ClientMetricData> getClientMetricsDataByUrl(MetricRegistry metricRegistry,
                                                                        String url){
        ArrayList<ClientTimer> timers = getClientMetricsByUrl(metricRegistry, url);
        return computeClientMetricsData(timers);
    }

    public static ArrayList<ClientMetricData> getClientMetricsDataByUrlAndMethod(MetricRegistry metricRegistry,
                                                                                 String url,
                                                                                 String method){
        ArrayList<ClientTimer> timers = getClientMetricsByUrlAndMethod(metricRegistry, url, method);
        return computeClientMetricsData(timers);
    }

    public static ArrayList<ClientMetricData> getClientMetricsDataByMetricId(MetricRegistry metricRegistry,
                                                                             String[] metricId){
        ArrayList<ClientTimer> timers = getClientMetricsByMetricId(metricRegistry, metricId);
        return computeClientMetricsData(timers);
    }
}
