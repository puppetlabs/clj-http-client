package com.puppetlabs.http.client;


import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.Promise;
import com.puppetlabs.http.client.impl.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class SyncHttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncHttpClient.class);


    private static void logAndRethrow(String msg, Throwable t) {
        LOGGER.error(msg, t);
        throw new HttpClientException(msg, t);
    }

    public static Response request(RequestOptions requestOptions, ClientOptions clientOptions) {
        // TODO: if we end up implementing an async version of the java API,
        // we should refactor this implementation so that it is based on the
        // async one, as Patrick has done in the clojure API.

        clientOptions = SslUtils.configureSsl(clientOptions);

        Promise<Response> promise =  JavaClient.request(requestOptions, clientOptions, null);

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


    public static Response get(String url) throws URISyntaxException {
        return get(new URI(url));
    }
    public static Response get(URI uri) {
        return get(new RequestOptions(uri), new ClientOptions());
    }
    public static Response get(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET), clientOptions);
    }

    public static Response head(String url) throws URISyntaxException {
        return head(new URI(url));
    }
    public static Response head(URI uri) {
        return head(new RequestOptions(uri), new ClientOptions());
    }
    public static Response head(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.HEAD), clientOptions);
    }

    public static Response post(String url) throws URISyntaxException {
        return post(new URI(url));
    }
    public static Response post(URI uri) { return post(new RequestOptions(uri), new ClientOptions()); }
    public static Response post(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.POST), clientOptions);
    }

    public static Response put(String url) throws URISyntaxException {
        return put(new URI(url));
    }
    public static Response put(URI uri) { return put(new RequestOptions(uri), new ClientOptions()); }
    public static Response put(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.PUT), clientOptions);
    }

    public static Response delete(String url) throws URISyntaxException {
        return delete(new URI(url));
    }
    public static Response delete(URI uri) { return delete(new RequestOptions(uri), new ClientOptions()); }
    public static Response delete(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.DELETE), clientOptions);
    }

    public static Response trace(String url) throws URISyntaxException {
        return trace(new URI(url));
    }
    public static Response trace(URI uri) { return trace(new RequestOptions(uri), new ClientOptions()); }
    public static Response trace(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.TRACE), clientOptions);
    }

    public static Response options(String url) throws URISyntaxException {
        return options(new URI(url));
    }
    public static Response options(URI uri) { return options(new RequestOptions(uri), new ClientOptions()); }
    public static Response options(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.OPTIONS), clientOptions);
    }

    public static Response patch(String url) throws URISyntaxException {
        return patch(new URI(url));
    }
    public static Response patch(URI uri) { return patch(new RequestOptions(uri), new ClientOptions()); }
    public static Response patch(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.PATCH), clientOptions);
    }
}
