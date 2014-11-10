package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.HttpClientException;
import com.puppetlabs.http.client.Response;
import com.puppetlabs.http.client.RequestOptions;
import com.puppetlabs.http.client.HttpMethod;
import com.puppetlabs.http.client.Sync;
import com.puppetlabs.http.client.SyncHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class PersistentSyncHttpClient implements SyncHttpClient {
    private CloseableHttpAsyncClient client;
    private static final Logger LOGGER = LoggerFactory.getLogger(Sync.class);

    public PersistentSyncHttpClient(CloseableHttpAsyncClient client) {
        this.client = client;
    }

    private static void logAndRethrow(String msg, Throwable t) {
        LOGGER.error(msg, t);
        throw new HttpClientException(msg, t);
    }

    private Response request(RequestOptions requestOptions) {
        Promise<Response> promise =  JavaClient.requestWithClient(requestOptions, null, client, true);

        Response response = null;
        try {
            response = promise.deref();
        } catch (InterruptedException e) {
            logAndRethrow("Error while waiting for http response", e);
        }
        if (response.getError() != null) {
            logAndRethrow("Error executing http request", response.getError());
        }
        return response;
    }

    public void close() {
        AsyncClose.close(client);
    }

    public Response get(String url) throws URISyntaxException {
        return get(new URI(url));
    }
    public Response get(URI uri) {
        return get(new RequestOptions(uri));
    }
    public Response get(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET));
    }

    public Response head(String url) throws URISyntaxException {
        return head(new URI(url));
    }
    public Response head(URI uri) {
        return head(new RequestOptions(uri));
    }
    public Response head(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.HEAD));
    }

    public Response post(String url) throws URISyntaxException {
        return post(new URI(url));
    }
    public Response post(URI uri) {
        return post(new RequestOptions(uri));
    }
    public Response post(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.POST));
    }

    public Response put(String url) throws URISyntaxException {
        return put(new URI(url));
    }
    public Response put(URI uri) {
        return put(new RequestOptions(uri));
    }
    public Response put(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PUT));
    }

    public Response delete(String url) throws URISyntaxException {
        return delete(new URI(url));
    }
    public Response delete(URI uri) {
        return delete(new RequestOptions(uri));
    }
    public Response delete(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.DELETE));
    }

    public Response trace(String url) throws URISyntaxException {
        return trace(new URI(url));
    }
    public Response trace(URI uri) {
        return trace(new RequestOptions(uri));
    }
    public Response trace(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.TRACE));
    }

    public Response options(String url) throws URISyntaxException {
        return options(new URI(url));
    }
    public Response options(URI uri) {
        return options(new RequestOptions(uri));
    }
    public Response options(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.OPTIONS));
    }

    public Response patch(String url) throws URISyntaxException {
        return patch(new URI(url));
    }
    public Response patch(URI uri) {
        return patch(new RequestOptions(uri));
    }
    public Response patch(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PATCH));
    }
}
