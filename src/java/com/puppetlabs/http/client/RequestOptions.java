package com.puppetlabs.http.client;

import org.apache.http.nio.client.HttpAsyncClient;
//import org.httpkit.client.HttpClient;
//
//import org.httpkit.client.IFilter;
//import org.httpkit.client.MultipartEntity;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class RequestOptions {
    private HttpAsyncClient client = null;

    private URI uri;
    private HttpMethod method = null;
    private Map<String, String> headers;
    private Object body;
    private boolean decompressBody = true;
    private ResponseBodyType as = ResponseBodyType.STREAM;

    public RequestOptions (String url) throws URISyntaxException { this.uri = new URI(url); }
    public RequestOptions(URI uri) {
        this.uri = uri;
    }
    public RequestOptions (HttpAsyncClient client,
                           URI uri,
                           HttpMethod method,
                           Map<String, String> headers,
                           Object body,
                           boolean decompressBody,
                           ResponseBodyType as) {
        this.client = client;
        this.uri = uri;
        this.method = method;
        this.headers = headers;
        this.body = body;
        this.decompressBody = decompressBody;
        this.as = as;
    }

    public HttpAsyncClient getClient() {
        return client;
    }
    public RequestOptions setClient(HttpAsyncClient client) {
        this.client = client;
        return this;
    }

    public URI getUri() {
        return uri;
    }
    public RequestOptions setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public HttpMethod getMethod() {
        return method;
    }
    public RequestOptions setMethod(HttpMethod method) {
        this.method = method;
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
