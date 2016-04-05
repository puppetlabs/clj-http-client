package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.HttpClientException;
import com.puppetlabs.http.client.Response;
import com.puppetlabs.http.client.RequestOptions;
import com.puppetlabs.http.client.HttpMethod;
import com.puppetlabs.http.client.SyncHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class PersistentSyncHttpClient implements SyncHttpClient {
    private CloseableHttpAsyncClient client;
    private MetricRegistry metricRegistry;
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentSyncHttpClient.class);

    public PersistentSyncHttpClient(CloseableHttpAsyncClient client, MetricRegistry metricRegistry) {
        this.client = client;
        this.metricRegistry = metricRegistry;
    }

    private static void logAndRethrow(String msg, Throwable t) {
        LOGGER.error(msg, t);
        throw new HttpClientException(msg, t);
    }

    public Map<String, Timer> getClientMetrics(){
        return Metrics.getClientMetrics(metricRegistry);
    }

    public Map<String, Timer> getClientMetrics(String url, Metrics.MetricType metricType) {
        return Metrics.getClientMetricsWithUrl(metricRegistry, url, metricType);
    }

    public Map<String, Timer> getClientMetrics(String url, String method, Metrics.MetricType metricType) {
        return Metrics.getClientMetricsWithUrlAndMethod(metricRegistry, url, method, metricType);
    }

    public Map<String, Timer> getClientMetrics(String[] metricId, Metrics.MetricType metricType) {
        return Metrics.getClientMetricsWithMetricId(metricRegistry, metricId, metricType);
    }

    public Map<String, ClientMetricData> getClientMetricsData(){
        return Metrics.getClientMetricsData(metricRegistry);
    }

    public Map<String, ClientMetricData> getClientMetricsData(String url, Metrics.MetricType metricType) {
        return Metrics.getClientMetricsDataWithUrl(metricRegistry, url, metricType);
    }

    public Map<String, ClientMetricData> getClientMetricsData(String url, String method, Metrics.MetricType metricType) {
        return Metrics.getClientMetricsDataWithUrlAndMethod(metricRegistry, url, method, metricType);
    }

    public Map<String, ClientMetricData> getClientMetricsData(String[] metricId, Metrics.MetricType metricType) {
        return Metrics.getClientMetricsDataWithMetricId(metricRegistry, metricId, metricType);
    }

    public Response request(RequestOptions requestOptions, HttpMethod method) {
        final Promise<Response> promise = new Promise<>();
        final JavaResponseDeliveryDelegate responseDelivery = new JavaResponseDeliveryDelegate(promise);
        JavaClient.requestWithClient(requestOptions, method, null, client, responseDelivery, metricRegistry);
        Response response = null;
        try {
            response = promise.deref();
            if (response.getError() != null) {
                logAndRethrow("Error executing http request", response.getError());
            }
        } catch (InterruptedException e) {
            logAndRethrow("Error while waiting for http response", e);
        }
        return response;
    }

    public void close() throws IOException {
        client.close();
    }

    public Response get(String url) throws URISyntaxException {
        return get(new URI(url));
    }
    public Response get(URI uri) {
        return get(new RequestOptions(uri));
    }
    public Response get(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.GET);
    }

    public Response head(String url) throws URISyntaxException {
        return head(new URI(url));
    }
    public Response head(URI uri) {
        return head(new RequestOptions(uri));
    }
    public Response head(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.HEAD);
    }

    public Response post(String url) throws URISyntaxException {
        return post(new URI(url));
    }
    public Response post(URI uri) {
        return post(new RequestOptions(uri));
    }
    public Response post(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.POST);
    }

    public Response put(String url) throws URISyntaxException {
        return put(new URI(url));
    }
    public Response put(URI uri) {
        return put(new RequestOptions(uri));
    }
    public Response put(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.PUT);
    }

    public Response delete(String url) throws URISyntaxException {
        return delete(new URI(url));
    }
    public Response delete(URI uri) {
        return delete(new RequestOptions(uri));
    }
    public Response delete(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.DELETE);
    }

    public Response trace(String url) throws URISyntaxException {
        return trace(new URI(url));
    }
    public Response trace(URI uri) {
        return trace(new RequestOptions(uri));
    }
    public Response trace(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.TRACE);
    }

    public Response options(String url) throws URISyntaxException {
        return options(new URI(url));
    }
    public Response options(URI uri) {
        return options(new RequestOptions(uri));
    }
    public Response options(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.OPTIONS);
    }

    public Response patch(String url) throws URISyntaxException {
        return patch(new URI(url));
    }
    public Response patch(URI uri) {
        return patch(new RequestOptions(uri));
    }
    public Response patch(RequestOptions requestOptions) {
        return request(requestOptions, HttpMethod.PATCH);
    }
}
