package com.puppetlabs.http.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * This class is a wrapper around a number of options for use in
 * configuring an HTTP request.
 */
public class RequestOptions {
    private URI uri;
    private Map<String, String> headers;
    private Object body;
    private boolean decompressBody = true;
    private ResponseBodyType as = ResponseBodyType.STREAM;
    private String[] metricId;

    /**
     * Constructor for the RequestOptions class. When this constructor is called,
     * decompressBody will default to true, and as will default to ResponseBodyType.STREAM
     * @param url the URL against which to make the request
     * @throws URISyntaxException
     */
    public RequestOptions (String url) throws URISyntaxException { this.uri = new URI(url); }

    /**
     * Constructor for the RequestOptions class. When this constructor is called,
     * decompressBody will default to true, and as will default to ResponseBodyType.STREAM
     * @param uri the URI against which to make the request
     */
    public RequestOptions(URI uri) {
        this.uri = uri;
    }

    /**
     * Constructor for the RequestOptions class
     * @param uri the URI against which to make the request
     * @param headers A map of headers. Can be null.
     * @param body The body of the request. Can be null.
     * @param decompressBody If true, an "accept-encoding" header with a value of "gzip, deflate" will be
     *                       automatically decompressed if it contains a recognized "content-encoding" header.
     * @param as Used to control the data type of the response body.
     */
    public RequestOptions (URI uri,
                           Map<String, String> headers,
                           Object body,
                           boolean decompressBody,
                           ResponseBodyType as) {
        this.uri = uri;
        this.headers = headers;
        this.body = body;
        this.decompressBody = decompressBody;
        this.as = as;
    }

    public URI getUri() {
        return uri;
    }
    public RequestOptions setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    public RequestOptions setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public Object getBody() {
        return body;
    }
    public RequestOptions setBody(Object body) {
        this.body = body;
        return this;
    }

    public boolean getDecompressBody() { return decompressBody; }
    public RequestOptions setDecompressBody(boolean decompressBody) {
        this.decompressBody = decompressBody;
        return this;
    }

    public ResponseBodyType getAs() {
        return as;
    }
    public RequestOptions setAs(ResponseBodyType as) {
        this.as = as;
        return this;
    }

    public String[] getMetricId() {
        return metricId;
    }

    public RequestOptions setMetricId(String[] metricId) {
        this.metricId = metricId;
        return this;
    }
}
