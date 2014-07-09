package com.puppetlabs.http.client;

import org.apache.http.nio.client.HttpAsyncClient;
//import org.httpkit.client.HttpClient;
//
//import org.httpkit.client.IFilter;
//import org.httpkit.client.MultipartEntity;

import javax.net.ssl.SSLContext;
import java.util.Map;

public class RequestOptions {
    private HttpAsyncClient client = null;

    private String url;
    private HttpMethod method = null;
    private Map<String, String> headers;
    private SSLContext sslContext;
    private String sslCert;
    private String sslKey;
    private String sslCaCert;
    private boolean insecure = false;
    private Object body;
    private boolean decompressBody = true;
    private ResponseBodyType as = ResponseBodyType.STREAM;

    public RequestOptions(String url) {
        this.url = url;
    }

    public HttpAsyncClient getClient() {
        return client;
    }
    public RequestOptions setClient(HttpAsyncClient client) {
        this.client = client;
        return this;
    }

    public String getUrl() {
        return url;
    }
    public RequestOptions setUrl(String url) {
        this.url = url;
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

    public SSLContext getSslContext() {
        return sslContext;
    }
    public RequestOptions setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public String getSslCert() {
        return sslCert;
    }
    public RequestOptions setSslCert(String sslCert) {
        this.sslCert = sslCert;
        return this;
    }

    public String getSslKey() {
        return sslKey;
    }
    public RequestOptions setSslKey(String sslKey) {
        this.sslKey = sslKey;
        return this;
    }

    public String getSslCaCert() {
        return sslCaCert;
    }
    public RequestOptions setSslCaCert(String sslCaCert) {
        this.sslCaCert = sslCaCert;
        return this;
    }

    public boolean getInsecure() {
        return insecure;
    }
    public RequestOptions setInsecure(boolean insecure) {
        this.insecure = insecure;
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
