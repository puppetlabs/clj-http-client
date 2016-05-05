package com.puppetlabs.http.client.metrics;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.puppetlabs.http.client.impl.metrics.CategoryClientTimerMetricFilter;
import com.puppetlabs.http.client.impl.metrics.ClientMetricFilter;
import com.puppetlabs.http.client.impl.metrics.TimerMetricData;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    private static List<UrlClientTimer> getUrlClientTimerArray(MetricRegistry registry,
                                                               MetricFilter filter) {
        List<UrlClientTimer> timerArray = new ArrayList<>();
        for (Map.Entry<String, Timer> entry : registry.getTimers(filter).entrySet()) {
            UrlClientTimer timer = (UrlClientTimer)entry.getValue();
            timerArray.add(timer);
        }
        return timerArray;
    }

    private static List<UrlAndMethodClientTimer> getUrlAndMethodClientTimerArray(MetricRegistry registry,
                                                                                 MetricFilter filter) {
        List<UrlAndMethodClientTimer> timerArray = new ArrayList<>();
        for (Map.Entry<String, Timer> entry : registry.getTimers(filter).entrySet()) {
            UrlAndMethodClientTimer timer = (UrlAndMethodClientTimer)entry.getValue();
            timerArray.add(timer);
        }
        return timerArray;
    }

    private static List<MetricIdClientTimer> getMetricIdClientTimerArray(MetricRegistry registry,
                                                                         MetricFilter filter) {
        List<MetricIdClientTimer> timerArray = new ArrayList<>();
        for (Map.Entry<String, Timer> entry : registry.getTimers(filter).entrySet()) {
            MetricIdClientTimer timer = (MetricIdClientTimer)entry.getValue();
            timerArray.add(timer);
        }
        return timerArray;
    }

    public static ClientTimerContainer getClientMetrics(MetricRegistry metricRegistry){
        if (metricRegistry != null) {
            return new ClientTimerContainer(
                    getUrlClientTimerArray(metricRegistry,
                            new CategoryClientTimerMetricFilter(URL_NAMESPACE)),
                    getUrlAndMethodClientTimerArray(metricRegistry,
                            new CategoryClientTimerMetricFilter(URL_METHOD_NAMESPACE)),
                    getMetricIdClientTimerArray(metricRegistry,
                            new CategoryClientTimerMetricFilter(ID_NAMESPACE)));
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static List<UrlClientTimer> getClientMetricsByUrl(MetricRegistry metricRegistry,
                                                               final String url){
        if (metricRegistry != null) {
            return getUrlClientTimerArray(metricRegistry,
                    new ClientMetricFilter(url, null, null, MetricType.FULL_RESPONSE));
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static List<UrlAndMethodClientTimer> getClientMetricsByUrlAndMethod(MetricRegistry metricRegistry,
                                                                        final String url,
                                                                        final String method){
        if (metricRegistry != null) {
            return getUrlAndMethodClientTimerArray(metricRegistry,
                    new ClientMetricFilter(url, method, null, MetricType.FULL_RESPONSE));
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static List<MetricIdClientTimer> getClientMetricsByMetricId(MetricRegistry metricRegistry,
                                                                    final String[] metricId){
        if (metricRegistry != null) {
            if (metricId.length == 0) {
                return getMetricIdClientTimerArray(metricRegistry,
                        new CategoryClientTimerMetricFilter(ID_NAMESPACE));
            } else {
                return getMetricIdClientTimerArray(metricRegistry,
                        new ClientMetricFilter(null, null,
                                new ArrayList<String>(Arrays.asList(metricId)),
                                MetricType.FULL_RESPONSE));
            }
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    private static List<UrlClientMetricData> computeUrlClientMetricsData(List<UrlClientTimer> timers) {
        if (timers != null) {
            List<UrlClientMetricData> metricsData = new ArrayList<>();
            for (UrlClientTimer timer: timers) {
                TimerMetricData timerMetricData = TimerMetricData.fromTimer(timer);
                String url = timer.getUrl();

                metricsData.add(new UrlClientMetricData(timerMetricData, url));
            }
            return metricsData;
        } else {
            return null;
        }
    }

    private static List<UrlAndMethodClientMetricData> computeUrlAndMethodClientMetricsData(List<UrlAndMethodClientTimer> timers) {
        if (timers != null) {
            List<UrlAndMethodClientMetricData> metricsData = new ArrayList<>();
            for (UrlAndMethodClientTimer timer: timers) {
                TimerMetricData timerMetricData = TimerMetricData.fromTimer(timer);
                String url = timer.getUrl();
                String method = timer.getMethod();

                metricsData.add(new UrlAndMethodClientMetricData(timerMetricData, url, method));
            }
            return metricsData;
        } else {
            return null;
        }
    }

    private static List<MetricIdClientMetricData> computeMetricIdClientMetricsData(List<MetricIdClientTimer> timers) {
        if (timers != null) {
            List<MetricIdClientMetricData> metricsData = new ArrayList<>();
            for (MetricIdClientTimer timer: timers) {
                TimerMetricData timerMetricData = TimerMetricData.fromTimer(timer);
                List<String> metricId = timer.getMetricId();

                metricsData.add(new MetricIdClientMetricData(timerMetricData, metricId));
            }
            return metricsData;
        } else {
            return null;
        }
    }

    public static ClientMetricDataContainer getClientMetricsData(MetricRegistry metricRegistry){
        if ( metricRegistry != null ) {
            ClientTimerContainer timers = getClientMetrics(metricRegistry);
            return new ClientMetricDataContainer(computeUrlClientMetricsData(timers.getUrlTimers()), computeUrlAndMethodClientMetricsData(timers.getUrlAndMethodTimers()), computeMetricIdClientMetricsData(timers.getMetricIdTimers())
            );
        } else {
            throw new IllegalArgumentException("Metric registry must not be null");
        }
    }

    public static List<UrlClientMetricData> getClientMetricsDataByUrl(MetricRegistry metricRegistry,
                                                                        String url){
        List<UrlClientTimer> timers = getClientMetricsByUrl(metricRegistry, url);
        return computeUrlClientMetricsData(timers);
    }

    public static List<UrlAndMethodClientMetricData> getClientMetricsDataByUrlAndMethod(MetricRegistry metricRegistry,
                                                                                 String url,
                                                                                 String method){
        List<UrlAndMethodClientTimer> timers = getClientMetricsByUrlAndMethod(metricRegistry, url, method);
        return computeUrlAndMethodClientMetricsData(timers);
    }

    public static List<MetricIdClientMetricData> getClientMetricsDataByMetricId(MetricRegistry metricRegistry,
                                                                                String[] metricId){
        List<MetricIdClientTimer> timers = getClientMetricsByMetricId(metricRegistry, metricId);
        return computeMetricIdClientMetricsData(timers);
    }
}
