package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.HttpMethod;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import javax.net.ssl.SSLContext;
import java.net.URI;

public class CoercedRequestOptions {
    private final URI uri;
    private final HttpMethod method;
    private final Header[] headers;
    private final HttpEntity body;
    private final SSLContext sslContext;


    public CoercedRequestOptions(URI uri,
                                 HttpMethod method,
                                 Header[] headers,
                                 HttpEntity body,
                                 SSLContext sslContext) {
        this.uri = uri;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.sslContext = sslContext;
    }

    public URI getUri() {
        return uri;
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
