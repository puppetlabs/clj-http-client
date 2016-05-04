package com.puppetlabs.http.client.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.puppetlabs.http.client.impl.metrics.ClientMetricFilter;

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

    public static String urlToMetricUrl(String uriString) throws URISyntaxException {
        final URI uri = new URI(uriString);
        final URI convertedUri = new URI(uri.getScheme(), null, uri.getHost(),
                uri.getPort(), uri.getPath(), null, null);
        return convertedUri.toString();
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
