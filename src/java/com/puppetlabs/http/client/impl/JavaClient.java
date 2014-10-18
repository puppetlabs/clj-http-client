package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.*;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class JavaClient {

    private static final String PROTOCOL = "TLS";

    private static Header[] prepareHeaders(RequestOptions options,
                                           ContentType contentType) {
        Map<String, Header> result = new HashMap<String, Header>();
        Map<String, String> origHeaders = options.getHeaders();
        if (origHeaders == null) {
            origHeaders = new HashMap<String, String>();
        }
        for (Map.Entry<String, String> entry : origHeaders.entrySet()) {
            result.put(entry.getKey().toLowerCase(), new BasicHeader(entry.getKey(), entry.getValue()));
        }
        if (options.getDecompressBody() &&
                (! result.containsKey("accept-encoding"))) {
            result.put("accept-encoding", new BasicHeader("Accept-Encoding", "gzip, deflate"));
        }

        if (contentType != null) {
            result.put("content-type", new BasicHeader("Content-Type",
                    contentType.toString()));
        }

        return result.values().toArray(new Header[result.size()]);
    }

    private static ContentType getContentType (RequestOptions options) {
        ContentType contentType = null;

        Map<String, String> headers = options.getHeaders();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase().equals("content-type")) {
                    String contentTypeValue = entry.getValue();
                    if (contentTypeValue != null && !contentTypeValue.isEmpty()) {
                        try {
                            contentType = ContentType.parse(contentTypeValue);
                        }
                        catch (ParseException e) {
                            throw new HttpClientException(
                                    "Unable to parse request content type", e);
                        }
                        catch (UnsupportedCharsetException e) {
                            throw new HttpClientException(
                                    "Unsupported content type charset", e);
                        }
                        if (contentType.getCharset() == null) {
                            contentType = ContentType.create(
                                    contentType.getMimeType(),
                                    Consts.UTF_8);
                        }
                    }
                }
            }
        }

        return contentType;
    }

    private static CoercedRequestOptions coerceRequestOptions(RequestOptions options) {
        URI uri = options.getUri();

        SSLContext sslContext = null;
        if (options.getSslContext() != null) {
            sslContext = options.getSslContext();
        } else if (options.getInsecure()) {
            sslContext = getInsecureSslContext();
        }

        String[] sslProtocols = null;
        if (options.getSslProtocols() != null) {
            sslProtocols = options.getSslProtocols();
        } else {
            sslProtocols = RequestOptions.DEFAULT_SSL_PROTOCOLS;
        }

        String[] sslCipherSuites = null;
        if (options.getSslCipherSuites() != null) {
            sslCipherSuites = options.getSslCipherSuites();
        }

        HttpMethod method = options.getMethod();
        if (method == null) {
            method = HttpMethod.GET;
        }

        ContentType contentType = getContentType(options);

        Header[] headers = prepareHeaders(options, contentType);

        HttpEntity body = null;

        if (options.getBody() instanceof String) {
            String originalBody = (String) options.getBody();
            if (contentType != null) {
                body = new NStringEntity(originalBody, contentType);
            }
            else {
                try {
                    body = new NStringEntity(originalBody);
                }
                catch (UnsupportedEncodingException e) {
                    throw new HttpClientException(
                            "Unable to create request body", e);
                }
            }

        } else if (options.getBody() instanceof InputStream) {
            body = new InputStreamEntity((InputStream)options.getBody());
        }

        boolean forceRedirects = options.getForceRedirects();
        boolean followRedirects = options.getFollowRedirects();

        return new CoercedRequestOptions(uri, method, headers, body, sslContext, sslProtocols, sslCipherSuites, forceRedirects, followRedirects);
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
                coercedOptions.getUri(), coercedOptions.getBody());
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
        HttpAsyncClientBuilder clientBuilder = HttpAsyncClients.custom();
        if (coercedOptions.getSslContext() != null) {
            clientBuilder.setSSLStrategy(
                    new SSLIOSessionStrategy(coercedOptions.getSslContext(),
                            coercedOptions.getSslProtocols(),
                            coercedOptions.getSslCipherSuites(),
                            SSLIOSessionStrategy.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER));
        }
        RedirectStrategy redirectStrategy;
        if (!coercedOptions.getFollowRedirects()) {
            redirectStrategy = new RedirectStrategy() {
                @Override
                public boolean isRedirected(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException {
                    return false;
                }

                @Override
                public HttpUriRequest getRedirect(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws ProtocolException {
                    return null;
                }
            };
        }
        else if (coercedOptions.getForceRedirects()) {
            redirectStrategy = new LaxRedirectStrategy();
        }
        else {
            redirectStrategy = new DefaultRedirectStrategy();
        }
        clientBuilder.setRedirectStrategy(redirectStrategy);
        CloseableHttpAsyncClient client = clientBuilder.build();
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

    private static HttpRequestBase constructRequest(HttpMethod httpMethod, URI uri, HttpEntity body) {
        switch (httpMethod) {
            case GET:
                return requestWithNoBody(new HttpGet(uri), body, httpMethod);
            case HEAD:
                return requestWithNoBody(new HttpHead(uri), body, httpMethod);
            case POST:
                return requestWithBody(new HttpPost(uri), body);
            case PUT:
                return requestWithBody(new HttpPut(uri), body);
            case DELETE:
                return requestWithNoBody(new HttpDelete(uri), body, httpMethod);
            case TRACE:
                return requestWithNoBody(new HttpTrace(uri), body, httpMethod);
            case OPTIONS:
                return requestWithNoBody(new HttpOptions(uri), body, httpMethod);
            case PATCH:
                return requestWithBody(new HttpPatch(uri), body);
            default:
                throw new HttpClientException("Unable to construct request for:" + httpMethod +
                                                ", " + uri.toString(), null);
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

    public static Object coerceBodyType(InputStream body, ResponseBodyType as,
                                         ContentType contentType) {
        switch (as) {
            case TEXT:
                String charset = "UTF-8";
                if ((contentType != null) && (contentType.getCharset() != null)) {
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
