package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.HttpMethod;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import javax.net.ssl.SSLContext;

public class CoercedRequestOptions {
    private final String url;
    private final HttpMethod method;
    private final Header[] headers;
    private final HttpEntity body;
    private final SSLContext sslContext;


    public CoercedRequestOptions(String url,
                                 HttpMethod method,
                                 Header[] headers,
                                 HttpEntity body,
                                 SSLContext sslContext) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.sslContext = sslContext;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public HttpEntity getBody() {
        return body;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }
}
