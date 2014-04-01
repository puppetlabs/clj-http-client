package com.puppetlabs.http.client;

import com.puppetlabs.http.client.HttpResponse;
import com.puppetlabs.http.client.RequestOptions;

import java.util.Map;

public class HttpResponse {
    private RequestOptions options;
    private Throwable error;
    private Object body;
    private Map<String, Object> headers;
    private Integer status;

    public HttpResponse(RequestOptions options, Throwable error) {
        this.options = options;
        this.error = error;
    }

    public HttpResponse(RequestOptions options, Object body, Map<String, Object> headers, int status) {
        this.options = options;
        this.body = body;
        this.headers = headers;
        this.status = status;
    }

    public RequestOptions getOptions() {
        return options;
    }

    public Throwable getError() {
        return error;
    }

    public Object getBody() {
        return body;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public Integer getStatus() {
        return status;
    }
}
