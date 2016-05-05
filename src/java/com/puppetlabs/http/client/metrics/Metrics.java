package com.puppetlabs.http.client.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.puppetlabs.http.client.impl.metrics.ClientMetricFilter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Metrics {
    public static final String METRIC_NAMESPACE = "puppetlabs.http-client.experimental";
    public static final String URL_NAMESPACE = "with-url";
    public static final String URL_METHOD_NAMESPACE = "with-url-and-method";
    public static final String ID_NAMESPACE = "with-metric-id";
    public static final String FULL_RESPONSE_STRING = "full-response";
    public enum MetricType { FULL_RESPONSE; }

    public static String urlToMetricUrl(String uriString) throws URISyntaxException {
        final URI uri = new URI(uriString);
        final URI convertedUri = new URI(uri.getScheme(), null, uri.getHost(),
                uri.getPort(), uri.getPath(), null, null);
        return convertedUri.toString();
    }

    public static List<ClientTimer> getClientTimerArray(Map<String, Timer> timers){
        List<ClientTimer> timerArray = new ArrayList<>();
        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            ClientTimer timer = (ClientTimer)entry.getValue();
            timerArray.add(timer);
        }
        return timerArray;
    }

    public static Map<String, List<ClientTimer>> getClientMetrics(MetricRegistry metricRegistry){
        if (metricRegistry != null) {
            Map<String, List<ClientTimer>> timers = new HashMap<>();
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

    public static List<ClientTimer> getClientMetricsByUrl(MetricRegistry metricRegistry,
                                                               final String url){
        if (metricRegistry != null) {
            Map<String, Timer> timers = metricRegistry.getTimers(
                    new ClientMetricFilter(url, null, null, MetricType.FULL_RESPONSE));
            return getClientTimerArray(timers);
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static List<ClientTimer> getClientMetricsByUrlAndMethod(MetricRegistry metricRegistry,
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

    public static List<ClientTimer> getClientMetricsByMetricId(MetricRegistry metricRegistry,
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

    public static List<ClientMetricData> computeClientMetricsData(List<ClientTimer> timers){
        if (timers != null) {
            List<ClientMetricData> metricsData = new ArrayList<>();
            for (ClientTimer timer: timers) {
                Double mean = timer.getSnapshot().getMean();
                Long meanMillis = TimeUnit.NANOSECONDS.toMillis(mean.longValue());
                Long count = timer.getCount();
                Long aggregate = count * meanMillis;
                String metricName = timer.getMetricName();
                // TODO: create subclasses of ClientMetricData to prevent null values from being necessary,
                // refactor into methods with types in signatures to get rid of instanceof stuff.
                String url = timer instanceof UrlClientTimer ? ((UrlClientTimer)(timer)).getUrl() : null;
                String method = timer instanceof UrlAndMethodClientTimer ? ((UrlAndMethodClientTimer)(timer)).getMethod() : null;
                List<String> metricId = timer instanceof MetricIdClientTimer ? ((MetricIdClientTimer)(timer)).getMetricId() : null;

                if (metricId != null) {
                    metricsData.add(new MetricIdClientMetricData(metricName, count, meanMillis, aggregate, metricId));
                } else if (method != null) {
                    metricsData.add(new UrlAndMethodClientMetricData(metricName, count, meanMillis, aggregate, url, method));
                } else {
                    metricsData.add(new UrlClientMetricData(metricName, count, meanMillis, aggregate, url));
                }
            }
            return metricsData;
        } else {
            return null;
        }
    }

    public static Map<String, List<ClientMetricData>> getClientMetricsData(MetricRegistry metricRegistry){
        if ( metricRegistry != null ) {
            Map<String, List<ClientTimer>> timers = getClientMetrics(metricRegistry);
            Map<String, List<ClientMetricData>> data = new HashMap<>();
            data.put("url", computeClientMetricsData(timers.get("url")));
            data.put("url-and-method", computeClientMetricsData(timers.get("url-and-method")));
            data.put("metric-id", computeClientMetricsData(timers.get("metric-id")));
            return data;
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static List<ClientMetricData> getClientMetricsDataByUrl(MetricRegistry metricRegistry,
                                                                        String url){
        List<ClientTimer> timers = getClientMetricsByUrl(metricRegistry, url);
        return computeClientMetricsData(timers);
    }

    public static List<ClientMetricData> getClientMetricsDataByUrlAndMethod(MetricRegistry metricRegistry,
                                                                                 String url,
                                                                                 String method){
        List<ClientTimer> timers = getClientMetricsByUrlAndMethod(metricRegistry, url, method);
        return computeClientMetricsData(timers);
    }

    public static List<ClientMetricData> getClientMetricsDataByMetricId(MetricRegistry metricRegistry,
                                                                             String[] metricId){
        List<ClientTimer> timers = getClientMetricsByMetricId(metricRegistry, metricId);
        return computeClientMetricsData(timers);
    }
}
