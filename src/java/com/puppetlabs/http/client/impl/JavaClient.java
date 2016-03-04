package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.ClientOptions;
import com.puppetlabs.http.client.HttpClientException;
import com.puppetlabs.http.client.HttpMethod;
import com.puppetlabs.http.client.RequestOptions;
import com.puppetlabs.http.client.ResponseBodyType;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.RequestLine;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.UnsupportedCharsetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class JavaClient {

    private static final String PROTOCOL = "TLS";
    public static final String METRIC_NAMESPACE = "puppetlabs.http-client";

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

    public static ContentType getContentType (Object body, RequestOptions options) {
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
                            throw new HttpClientException("Unable to parse request content type", e);
                        }
                        catch (UnsupportedCharsetException e) {
                            throw new HttpClientException("Unsupported content type charset", e);
                        }
                        // In the case when the caller provides the body as a string, and does not
                        // specify a charset, we choose one for them.  There will always be _some_
                        // charset used to encode the string, and in this case we choose UTF-8
                        // (instead of letting the underlying Apache HTTP client library
                        // choose ISO-8859-1) because UTF-8 is a more reasonable default.
                        if (contentType.getCharset() == null && body instanceof String) {
                            contentType = ContentType.create(contentType.getMimeType(), Consts.UTF_8);
                        }
                    }
                }
            }
        }

        return contentType;
    }

    private static CoercedRequestOptions coerceRequestOptions(RequestOptions options, HttpMethod method) {
        URI uri = options.getUri();

        if (method == null) {
            method = HttpMethod.GET;
        }

        ContentType contentType = getContentType(options.getBody(), options);

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

        return new CoercedRequestOptions(uri, method, headers, body);
    }

    public static CoercedClientOptions coerceClientOptions(ClientOptions options) {
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
            sslProtocols = ClientOptions.DEFAULT_SSL_PROTOCOLS;
        }

        String[] sslCipherSuites = null;
        if (options.getSslCipherSuites() != null) {
            sslCipherSuites = options.getSslCipherSuites();
        }

        boolean forceRedirects = options.getForceRedirects();
        boolean followRedirects = options.getFollowRedirects();
        int connectTimeoutMilliseconds =
                options.getConnectTimeoutMilliseconds();
        int socketTimeoutMilliseconds =
                options.getSocketTimeoutMilliseconds();
        return new CoercedClientOptions(sslContext, sslProtocols,
                sslCipherSuites, forceRedirects, followRedirects,
                connectTimeoutMilliseconds, socketTimeoutMilliseconds);
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

    private static void completeResponse(ResponseDeliveryDelegate responseDeliveryDelegate,
                                         RequestOptions requestOptions,
                                         IResponseCallback callback,
                                         HttpResponse httpResponse,
                                         HttpContext httpContext) {
        try {
            Map<String, String> headers = new HashMap<>();
            for (Header h : httpResponse.getAllHeaders()) {
                headers.put(h.getName().toLowerCase(), h.getValue());
            }
            String origContentEncoding = headers.get("content-encoding");
            if (requestOptions.getDecompressBody()) {
                new ResponseContentEncoding().process(httpResponse, httpContext);
            }
            Object body = null;
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                body = entity.getContent();
            }
            ContentType contentType = null;
            String contentTypeValue = headers.get("content-type");
            if (contentTypeValue != null && !contentTypeValue.isEmpty()) {
                contentType = ContentType.parse(contentTypeValue);
            }
            if (requestOptions.getAs() == ResponseBodyType.TEXT) {
                body = coerceBodyType((InputStream) body, requestOptions.getAs(), contentType);
            }
            responseDeliveryDelegate.deliverResponse(requestOptions,
                    origContentEncoding,
                    body,
                    headers,
                    httpResponse.getStatusLine().getStatusCode(),
                    contentType,
                    callback);
        } catch (Exception e) {
            responseDeliveryDelegate.deliverResponse(requestOptions, e, callback);
        }
    }

    private static void executeWithConsumer(final CloseableHttpAsyncClient client,
                                            final FutureCallback<HttpResponse> futureCallback,
                                            final HttpRequestBase request,
                                            final MetricRegistry metricRegistry) {

        final TimedFutureCallback<HttpResponse> timedFutureCallback =
                new TimedFutureCallback<>(futureCallback, startTimer(metricRegistry, request));

        /*
         * Create an Apache AsyncResponseConsumer that will return the response to us as soon as it is available,
         * then send the response body asynchronously
         */
        final StreamingAsyncResponseConsumer consumer =
                new StreamingAsyncResponseConsumer(new Deliverable<HttpResponse>() {
            @Override
            public void deliver(HttpResponse httpResponse) {
                timedFutureCallback.completed(httpResponse); // this stops the timer for the request metric
            }
        });

        /*
         * Normally the consumer returns the response as soon as it is available using the deliver() callback (above)
         * which delegates to the supplied futureCallback.
         *
         * If an error occurs early in the request, the consumer may not get a chance to deliver the response. This
         * streamingCompleteCallback wraps the supplied futureCallback and ensures:
         *  - The supplied futureCallback is always eventually called even in error states
         *  - Any exception that occurs during stream processing (after the response has been returned) is propagated
         *    back to the client using the setFinalResult() method.
         */
        final FutureCallback<HttpResponse> streamingCompleteCallback = new
                FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse httpResponse) {
                        consumer.setFinalResult(null);

                        // this stops the timer on the metric for the streaming of the payload
                        futureCallback.completed(httpResponse);
                    }

                    @Override
                    public void failed(Exception e) {
                        if (e instanceof IOException) {
                            consumer.setFinalResult((IOException) e);
                        } else {
                            consumer.setFinalResult(new IOException(e));
                        }
                        futureCallback.failed(e);
                    }

                    @Override
                    public void cancelled() {
                        consumer.setFinalResult(null);
                        futureCallback.cancelled();
                    }
                };

        TimedFutureCallback<HttpResponse> timedStreamingCompleteCallback =
                new TimedFutureCallback<>(streamingCompleteCallback,
                        startUnbufferedStreamTimer(metricRegistry, request));
        client.execute(HttpAsyncMethods.create(request), consumer, timedStreamingCompleteCallback);
    }

    public static void requestWithClient(final RequestOptions requestOptions,
                                         final HttpMethod method,
                                         final IResponseCallback callback,
                                         final CloseableHttpAsyncClient client,
                                         final ResponseDeliveryDelegate responseDeliveryDelegate,
                                         final MetricRegistry registry) {

        final CoercedRequestOptions coercedRequestOptions = coerceRequestOptions(requestOptions, method);

        final HttpRequestBase request = constructRequest(coercedRequestOptions.getMethod(),
                coercedRequestOptions.getUri(), coercedRequestOptions.getBody());
        request.setHeaders(coercedRequestOptions.getHeaders());

        final HttpContext httpContext = HttpClientContext.create();

        final FutureCallback<HttpResponse> futureCallback = new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                completeResponse(responseDeliveryDelegate, requestOptions, callback, httpResponse, httpContext);
            }

            @Override
            public void failed(Exception e) {
                responseDeliveryDelegate.deliverResponse(requestOptions, e, callback);
            }

            @Override
            public void cancelled() {
                responseDeliveryDelegate.deliverResponse(requestOptions,
                        new HttpClientException("Request cancelled"),
                        callback);
            }
        };

        if (requestOptions.getAs() == ResponseBodyType.UNBUFFERED_STREAM) {
            executeWithConsumer(client, futureCallback, request, registry);
        } else {
            TimedFutureCallback<HttpResponse> timedFutureCallback =
                    new TimedFutureCallback<>(futureCallback, startTimer(registry, request));
            client.execute(request, timedFutureCallback);
        }
    }

    public static CloseableHttpAsyncClient createClient(ClientOptions clientOptions) {
        CoercedClientOptions coercedOptions = coerceClientOptions(SslUtils.configureSsl(clientOptions));
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

        RequestConfig requestConfig = getRequestConfig(coercedOptions);

        if (requestConfig != null) {
            clientBuilder.setDefaultRequestConfig(requestConfig);
        }

        CloseableHttpAsyncClient client = clientBuilder.build();
        client.start();
        return client;
    }

    private static RequestConfig getRequestConfig
            (CoercedClientOptions options) {
        RequestConfig config = null;

        int connectTimeoutMilliseconds = options.getConnectTimeoutMilliseconds();
        int socketTimeoutMilliseconds = options.getSocketTimeoutMilliseconds();

        if (connectTimeoutMilliseconds >= 0 || socketTimeoutMilliseconds >= 0) {
            Builder requestConfigBuilder = RequestConfig.custom();

            if (connectTimeoutMilliseconds >= 0) {
                requestConfigBuilder.setConnectTimeout
                        (connectTimeoutMilliseconds);
            }

            if (socketTimeoutMilliseconds >= 0) {
                requestConfigBuilder.setSocketTimeout
                        (socketTimeoutMilliseconds);
            }

            config = requestConfigBuilder.build();
        }

        return config;
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


    public static Object coerceBodyType(InputStream body, ResponseBodyType as,
                                         ContentType contentType) {
        Object response = null;

        switch (as) {
            case TEXT:
                String charset = "UTF-8";
                if ((contentType != null) && (contentType.getCharset() != null)) {
                    charset = contentType.getCharset().name();
                }
                try {
                    if (body == null){
                        response = "";
                    }
                    else{
                        response = IOUtils.toString(body, charset);
                    }
                } catch (IOException e) {
                    throw new HttpClientException("Unable to read body as string", e);
                }
                try {
                    if (body != null){
                        body.close();
                    }
                } catch (IOException e) {
                    throw new HttpClientException(
                            "Unable to close response stream", e);
                }
                break;
            default:
                throw new HttpClientException("Unsupported body type: " + as);
        }

        return response;
    }

    private static Timer.Context startTimer(MetricRegistry registry, HttpRequest request) {
        if (registry != null) {
            final RequestLine requestLine = request.getRequestLine();
            final String name = MetricRegistry.name(METRIC_NAMESPACE, requestLine.getUri(),
                    requestLine.getMethod());
            return registry.timer(name).time();
        } else {
            return null;
        }
    }

    private static Timer.Context startUnbufferedStreamTimer(MetricRegistry registry, HttpRequest request) {
        if (registry != null) {
            final RequestLine requestLine = request.getRequestLine();
            final String name = MetricRegistry.name(METRIC_NAMESPACE, requestLine.getUri(),
                    requestLine.getMethod(), "unbuffered_stream");
            return registry.timer(name).time();
        } else {
            return null;
        }
    }

    public static Map<String, Timer> getClientMetrics(MetricRegistry metricRegistry){
        if (metricRegistry != null) {
            return metricRegistry.getTimers(new ClientMetricFilter());
        } else {
            return null;
        }
    }

    public static Map<String, ClientMetricData> getClientMetricsData(MetricRegistry metricRegistry){
        Map<String, ClientMetricData> metricsData = new HashMap<>();
        Map<String, Timer> timers = getClientMetrics(metricRegistry);
        if (timers != null) {
            for (SortedMap.Entry<String, Timer> entry : timers.entrySet()) {
                Timer timer = entry.getValue();
                String metricId = entry.getKey();
                Double mean = timer.getSnapshot().getMean();
                Long meanMillis = TimeUnit.NANOSECONDS.toMillis(mean.longValue());
                Long count = timer.getCount();
                Long aggregate = count * meanMillis;

                ClientMetricData data = new ClientMetricData(metricId, count, meanMillis, aggregate);
                metricsData.put(metricId, data);
            }
        }
        return metricsData;
    }
}
