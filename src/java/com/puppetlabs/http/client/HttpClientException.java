package com.puppetlabs.http.client;

/**
 * This class represents an exception caused by an error in
 * an HTTP request
 *
 * @author Chris Price
 */
public class HttpClientException extends RuntimeException {
    public HttpClientException(String msg) {
        super(msg);
    }
    public HttpClientException(String msg, Throwable t) {
        super(msg, t);
    }
}
