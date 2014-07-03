package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.*;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class JavaClient {

    private static final String PROTOCOL = "TLS";

    private static Header[] prepareHeaders(RequestOptions options) {
        Map<String, Header> result = new HashMap<String, Header>();
        Map<String, String> origHeaders = options.getHeaders();
        if (origHeaders == null) {
            origHeaders = new HashMap<String, String>();
        }
        for (Map.Entry<String, String> entry : origHeaders.entrySet()) {
            result.put(entry.getKey().toLowerCase(), new BasicHeader(entry.getKey(), entry.getValue()));
        }
        if (options.getDecompressBody() &&
                (! origHeaders.containsKey("accept-encoding"))) {
            result.put("accept-encoding", new BasicHeader("Accept-Encoding", "gzip, deflate"));
        }
        return result.values().toArray(new Header[result.size()]);
    }

    private static CoercedRequestOptions coerceRequestOptions(RequestOptions options) {
        String url = options.getUrl();

        SSLContext sslContext = null;
        if (options.getSslContext() != null) {
            sslContext = options.getSslContext();
        } else if (options.getInsecure()) {
            sslContext = getInsecureSslContext();
        }

        HttpMethod method = options.getMethod();
        if (method == null) {
            method = HttpMethod.GET;
        }

        Header[] headers = prepareHeaders(options);

        HttpEntity body = null;

        if (options.getBody() instanceof String) {
            try {
                body = new NStringEntity((String)options.getBody());
            } catch (UnsupportedEncodingException e) {
                throw new HttpClientException("Unable to create request body", e);
            }
        } else if (options.getBody() instanceof InputStream) {
            body = new InputStreamEntity((InputStream)options.getBody());
        }

        return new CoercedRequestOptions(url, method, headers, body, sslContext);
    }

    private static SSLContext getInsecureSslContext() {
        SSLContext context = null;
        try {
            context = SSLContext.getInstance(PROTOCOL);
        } catch (NoSuchAlgorithmException e) {
            throw new HttpClientException("Unable to construct HTTP context", e);
        }
        try {
            context.init(null, new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                            // Always trust
                        }

                        public void checkServerTrusted(X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                            // Always trust
                        }
                    }},
                    null);
        } catch (KeyManagementException e) {
            throw new HttpClientException("Unable to initialize insecure SSL context", e);
        }
        return context;
    }

    public static Promise<Response> request(final RequestOptions options, final IResponseCallback callback) {
        CoercedRequestOptions coercedOptions = coerceRequestOptions(options);

        final CloseableHttpAsyncClient client = createClient(coercedOptions);

        HttpRequestBase request = constructRequest(coercedOptions.getMethod(),
                coercedOptions.getUrl(), coercedOptions.getBody());
        request.setHeaders(coercedOptions.getHeaders());

        final Promise<Response> promise = new Promise<Response>();

        client.execute(request, new FutureCallback<org.apache.http.HttpResponse>() {
            @Override
            public void completed(org.apache.http.HttpResponse httpResponse) {
                try {
                    Object body = null;
                    HttpEntity entity = httpResponse.getEntity();
                    if (entity != null) {
                        body = entity.getContent();
                    }
                    Map<String, String> headers = new HashMap<String, String>();
                    for (Header h : httpResponse.getAllHeaders()) {
                        headers.put(h.getName().toLowerCase(), h.getValue());
                    }
                    String origContentEncoding = headers.get("content-encoding");
                    if (options.getDecompressBody()) {
                        body = decompress((InputStream)body, headers);
                    }
                    ContentType contentType = null;
                    if (headers.get("content-type") != null) {
                        contentType = ContentType.parse(headers.get("content-type"));
                    }
                    if (options.getAs() != ResponseBodyType.STREAM) {
                        body = coerceBodyType((InputStream)body, options.getAs(), contentType);
                    }
                    deliverResponse(client, options,
                            new Response(options, origContentEncoding, body,
                                    headers, httpResponse.getStatusLine().getStatusCode(),
                                    contentType),
                            callback, promise);
                } catch (Exception e) {
                    deliverResponse(client, options, new Response(options, e), callback, promise);
                }
            }

            @Override
            public void failed(Exception e) {
                deliverResponse(client, options, new Response(options, e), callback, promise);
            }

            @Override
            public void cancelled() {
                deliverResponse(client, options, new Response(options, new HttpClientException("Request cancelled", null)), callback, promise);
            }
        });

        return promise;
    }

    private static CloseableHttpAsyncClient createClient(CoercedRequestOptions coercedOptions) {
        CloseableHttpAsyncClient client;
        if (coercedOptions.getSslContext() != null) {
            client = HttpAsyncClients.custom().setSSLContext(coercedOptions.getSslContext()).build();
        } else {
            client = HttpAsyncClients.createDefault();
        }
        client.start();
        return client;
    }

    private static void deliverResponse(CloseableHttpAsyncClient client, RequestOptions options,
                                        Response httpResponse, IResponseCallback callback,
                                        Promise<Response> promise) {
        try {
            if (callback != null) {
                try {
                    promise.deliver(callback.handleResponse(httpResponse));
                } catch (Exception ex) {
                    promise.deliver(new Response(options, ex));
                }
            } else {
                promise.deliver(httpResponse);
            }
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                throw new HttpClientException("Unable to close client", e);
            }
        }
    }

    private static HttpRequestBase constructRequest(HttpMethod httpMethod, String url, HttpEntity body) {
        switch (httpMethod) {
            case GET:
                return requestWithNoBody(new HttpGet(url), body, httpMethod);
            case HEAD:
                return requestWithNoBody(new HttpHead(url), body, httpMethod);
            case POST:
                return requestWithBody(new HttpPost(url), body);
            case PUT:
                return requestWithBody(new HttpPut(url), body);
            case DELETE:
                return requestWithNoBody(new HttpDelete(url), body, httpMethod);
            case TRACE:
                return requestWithNoBody(new HttpTrace(url), body, httpMethod);
            case OPTIONS:
                return requestWithNoBody(new HttpOptions(url), body, httpMethod);
            case PATCH:
                return requestWithBody(new HttpPatch(url), body);
            default:
                throw new HttpClientException("Unable to construct request for:" + httpMethod + ", " + url, null);
        }
    }

    private static HttpRequestBase requestWithBody(HttpEntityEnclosingRequestBase request, HttpEntity body) {
        if (body != null) {
            request.setEntity(body);
        }
        return request;
    }

    private static HttpRequestBase requestWithNoBody(HttpRequestBase request, Object body, HttpMethod httpMethod) {
        if (body != null) {
            throw new HttpClientException("Request of type " + httpMethod + " does not support 'body'!");
        }
        return request;
    }

    public static InputStream decompress(InputStream compressed, Map<String, String> headers) {
        String contentEncoding = headers.get("content-encoding");
        if (contentEncoding == null) {
            return compressed;
        }
        switch (contentEncoding) {
            case "gzip":
                headers.remove("content-encoding");
                return Compression.gunzip(compressed);
            case "deflate":
                headers.remove("content-encoding");
                return Compression.inflate(compressed);
            default:
                return compressed;
        }
    }

    private static Object coerceBodyType(InputStream body, ResponseBodyType as,
                                         ContentType contentType) {
        switch (as) {
            case TEXT:
                String charset = "UTF-8";
                if (contentType != null) {
                    charset = contentType.getCharset().name();
                }
                try {
                    return IOUtils.toString(body, charset);
                } catch (IOException e) {
                    throw new HttpClientException("Unable to read body as string", e);
                }
            default:
                throw new HttpClientException("Unsupported body type: " + as);
        }
    }

}
