package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.Promise;
import com.puppetlabs.http.client.impl.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncHttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncHttpClient.class);

    private static void logAndRethrow(String msg, Throwable t) {
        LOGGER.error(msg, t);
        throw new HttpClientException(msg, t);
    }

    public static Promise<Response> request(RequestOptions options) {
        options = SslUtils.configureSsl(options);

        return JavaClient.request(options, null);
    }
    
    public static Promise<Response> get(String url) {
        return get(new RequestOptions(url));
    }
    public static Promise<Response> get(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET));
    }

    public static Promise<Response> head(String url) {
        return head(new RequestOptions(url));
    }
    public static Promise<Response> head(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.HEAD));
    }

    public static Promise<Response> post(String url) {
        return post(new RequestOptions(url));
    }
    public static Promise<Response> post(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.POST));
    }

    public static Promise<Response> put(String url) {
        return put(new RequestOptions(url));
    }
    public static Promise<Response> put(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PUT));
    }

    public static Promise<Response> delete(String url) {
        return delete(new RequestOptions(url));
    }
    public static Promise<Response> delete(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.DELETE));
    }

    public static Promise<Response> trace(String url) {
        return trace(new RequestOptions(url));
    }
    public static Promise<Response> trace(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.TRACE));
    }

    public static Promise<Response> options(String url) {
        return options(new RequestOptions(url));
    }
    public static Promise<Response> options(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.OPTIONS));
    }

    public static Promise<Response> patch(String url) {
        return patch(new RequestOptions(url));
    }
    public static Promise<Response> patch(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PATCH));
    }
}
