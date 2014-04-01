package com.puppetlabs.http.client.impl;

import org.httpkit.HttpMethod;

import javax.net.ssl.SSLEngine;
import java.util.Map;

public class CoercedRequestOptions {
    private final String url;
    private final HttpMethod method;
    private final Map<String, Object> headers;
    private final Object body;
    private final SSLEngine sslEngine;


    public CoercedRequestOptions(String url,
                                 HttpMethod method,
                                 Map<String, Object> headers,
                                 Object body,
                                 SSLEngine sslEngine) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.sslEngine = sslEngine;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public Object getBody() {
        return body;
    }

    public SSLEngine getSslEngine() {
        return sslEngine;
    }
}
