package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.HttpMethod;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.net.URI;
import java.util.zip.GZIPOutputStream;

class CoercedRequestOptions {
    private final URI uri;
    private final HttpMethod method;
    private final Header[] headers;
    private final HttpEntity body;
    private final GZIPOutputStream gzipOutputStream;
    private final byte[] bytesToGzip;

    public CoercedRequestOptions(URI uri,
                                 HttpMethod method,
                                 Header[] headers,
                                 HttpEntity body,
                                 GZIPOutputStream gzipOutputStream,
                                 byte[] bytesToGzip) {
        this.uri = uri;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.gzipOutputStream = gzipOutputStream;
        this.bytesToGzip = bytesToGzip;
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

    public GZIPOutputStream getGzipOutputStream() { return gzipOutputStream; };

    public byte[] getBytesToGzip() { return bytesToGzip; }
}
