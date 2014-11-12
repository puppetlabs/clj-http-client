package com.puppetlabs.http.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class RequestOptions {
    private URI uri;
    private Map<String, String> headers;
    private Object body;
    private boolean decompressBody = true;
    private ResponseBodyType as = ResponseBodyType.STREAM;

    public RequestOptions (String url) throws URISyntaxException { this.uri = new URI(url); }
    public RequestOptions(URI uri) {
        this.uri = uri;
    }
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
}
