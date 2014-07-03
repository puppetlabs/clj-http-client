package com.puppetlabs.http.client;

public class HttpClientException extends RuntimeException {
    public HttpClientException(String msg) {
        super(msg);
    }
    public HttpClientException(String msg, Throwable t) {
        super(msg, t);
    }
}
