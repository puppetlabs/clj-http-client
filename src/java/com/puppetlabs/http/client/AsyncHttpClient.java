package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.Promise;
import com.puppetlabs.http.client.impl.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class AsyncHttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncHttpClient.class);

    public static Promise<Response> request(RequestOptions requestOptions, ClientOptions clientOptions) {
        clientOptions = SslUtils.configureSsl(clientOptions);

        return JavaClient.request(requestOptions, clientOptions, null);
    }
    
    public static Promise<Response> get(String url) throws URISyntaxException {
        return get(new URI(url));
    }
    public static Promise<Response> get(URI uri) {
        return get(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> get(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET), clientOptions);
    }

    public static Promise<Response> head(String url) throws URISyntaxException {
        return head(new URI(url));
    }
    public static Promise<Response> head(URI uri) {
        return head(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> head(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.HEAD), clientOptions);
    }

    public static Promise<Response> post(String url) throws URISyntaxException {
        return post(new URI(url));
    }
    public static Promise<Response> post(URI uri) {
        return post(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> post(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.POST), clientOptions);
    }

    public static Promise<Response> put(String url) throws URISyntaxException {
        return put(new URI(url));
    }
    public static Promise<Response> put(URI uri) {
        return put(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> put(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.PUT), clientOptions);
    }

    public static Promise<Response> delete(String url) throws URISyntaxException {
        return delete(new URI(url));
    }
    public static Promise<Response> delete(URI uri) {
        return delete(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> delete(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.DELETE), clientOptions);
    }

    public static Promise<Response> trace(String url) throws URISyntaxException {
        return trace(new URI(url));
    }
    public static Promise<Response> trace(URI uri) {
        return trace(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> trace(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.TRACE), clientOptions);
    }

    public static Promise<Response> options(String url) throws URISyntaxException {
        return options(new URI(url));
    }
    public static Promise<Response> options(URI uri) {
        return options(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> options(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.OPTIONS), clientOptions);
    }

    public static Promise<Response> patch(String url) throws URISyntaxException {
        return patch(new URI(url));
    }
    public static Promise<Response> patch(URI uri) {
        return patch(new RequestOptions(uri), new ClientOptions());
    }
    public static Promise<Response> patch(RequestOptions requestOptions, ClientOptions clientOptions) {
        return request(requestOptions.setMethod(HttpMethod.PATCH), clientOptions);
    }
}
