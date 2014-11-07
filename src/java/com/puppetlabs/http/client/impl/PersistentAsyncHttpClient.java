package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.*;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class PersistentAsyncHttpClient {
    private CloseableHttpAsyncClient client;
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncHttpClient.class);
    
    public PersistentAsyncHttpClient(CloseableHttpAsyncClient client) {
        this.client = client;
    }

    public void close() {
        AsyncClose.close(client);
    }

    public Promise<Response> request(RequestOptions requestOptions) {
        return JavaClient.requestWithClient(requestOptions, null, client, true);
    }

    public Promise<Response> get(String url) throws URISyntaxException {
        return get(new URI(url));
    }
    public Promise<Response> get(URI uri) {
        return get(new RequestOptions(uri));
    }
    public Promise<Response> get(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET));
    }

    public Promise<Response> head(String url) throws URISyntaxException {
        return head(new URI(url));
    }
    public Promise<Response> head(URI uri) {
        return head(new RequestOptions(uri));
    }
    public Promise<Response> head(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.HEAD));
    }

    public Promise<Response> post(String url) throws URISyntaxException {
        return post(new URI(url));
    }
    public Promise<Response> post(URI uri) {
        return post(new RequestOptions(uri));
    }
    public Promise<Response> post(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.POST));
    }

    public Promise<Response> put(String url) throws URISyntaxException {
        return put(new URI(url));
    }
    public Promise<Response> put(URI uri) {
        return put(new RequestOptions(uri));
    }
    public Promise<Response> put(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PUT));
    }

    public Promise<Response> delete(String url) throws URISyntaxException {
        return delete(new URI(url));
    }
    public Promise<Response> delete(URI uri) {
        return delete(new RequestOptions(uri));
    }
    public Promise<Response> delete(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.DELETE));
    }

    public Promise<Response> trace(String url) throws URISyntaxException {
        return trace(new URI(url));
    }
    public Promise<Response> trace(URI uri) {
        return trace(new RequestOptions(uri));
    }
    public Promise<Response> trace(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.TRACE));
    }

    public Promise<Response> options(String url) throws URISyntaxException {
        return options(new URI(url));
    }
    public Promise<Response> options(URI uri) {
        return options(new RequestOptions(uri));
    }
    public Promise<Response> options(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.OPTIONS));
    }

    public Promise<Response> patch(String url) throws URISyntaxException {
        return patch(new URI(url));
    }
    public Promise<Response> patch(URI uri) {
        return patch(new RequestOptions(uri));
    }
    public Promise<Response> patch(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PATCH));
    }
}
