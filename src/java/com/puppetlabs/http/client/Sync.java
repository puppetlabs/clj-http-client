package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.PersistentSyncHttpClient;
import com.puppetlabs.http.client.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * This class allows for the creation of a persistent synchronous HTTP client. It also allows
 * for sending synchronous HTTP requests without a persistent HTTP client.
 */
public class Sync {
    private static final Logger LOGGER = LoggerFactory.getLogger(Sync.class);


    private static void logAndRethrow(String msg, Throwable t) {
        LOGGER.error(msg, t);
        throw new HttpClientException(msg, t);
    }

    private static RequestOptions extractRequestOptions(SimpleRequestOptions simpleOptions) {
        URI uri = simpleOptions.getUri();
        Map<String, String> headers = simpleOptions.getHeaders();
        Object body = simpleOptions.getBody();
        boolean decompressBody = simpleOptions.getDecompressBody();
        ResponseBodyType as = simpleOptions.getAs();
        CompressType requestBodyDecompression =
                simpleOptions.getCompressRequestBody();
        RequestOptions requestOptions = new RequestOptions(
                uri, headers, body, decompressBody, as);
        requestOptions.setCompressRequestBody(requestBodyDecompression);
        return requestOptions;
    }

    private static ClientOptions extractClientOptions(SimpleRequestOptions simpleOptions) {
        SSLContext sslContext = simpleOptions.getSslContext();
        String sslCert = simpleOptions.getSslCert();
        String sslKey = simpleOptions.getSslKey();
        String sslCaCert = simpleOptions.getSslCaCert();
        String[] sslProtocols = simpleOptions.getSslProtocols();
        String[] sslCipherSuites = simpleOptions.getSslCipherSuites();
        boolean insecure = simpleOptions.getInsecure();
        boolean forceRedirects = simpleOptions.getForceRedirects();
        boolean followRedirects = simpleOptions.getFollowRedirects();
        int connectTimeoutMilliseconds =
                simpleOptions.getConnectTimeoutMilliseconds();
        int socketTimeoutMilliseconds =
                simpleOptions.getSocketTimeoutMilliseconds();

        return new ClientOptions(sslContext, sslCert, sslKey, sslCaCert,
                sslProtocols, sslCipherSuites, insecure,
                forceRedirects, followRedirects, connectTimeoutMilliseconds,
                socketTimeoutMilliseconds);
    }

    private static Response request(SimpleRequestOptions simpleRequestOptions,
                                    HttpMethod method) {
        // TODO: if we end up implementing an async version of the java API,
        // we should refactor this implementation so that it is based on the
        // async one, as Patrick has done in the clojure API.
        Response response = null;
        final SyncHttpClient client = createClient(
                extractClientOptions(simpleRequestOptions));
        try {
            response = client.request(
                    extractRequestOptions(simpleRequestOptions),
                    method);
        }
        finally {
            try {
                client.close();
            }
            catch (IOException e) {
                logAndRethrow("Error closing client", e);
            }
        }
        return response;
    }

    /**
     * Creates a synchronous persistent HTTP client
     * @param clientOptions
     * @return A persistent synchronous HTTP client
     */
    public static SyncHttpClient createClient(ClientOptions clientOptions) {
        final String metricNamespace = Metrics.buildMetricNamespace(clientOptions.getMetricPrefix(),
                clientOptions.getServerId());
        return new PersistentSyncHttpClient(JavaClient.createClient(clientOptions),
                clientOptions.getMetricRegistry(), metricNamespace, clientOptions.isEnableURLMetrics());
    }

    /**
     * Makes a simple HTTP GET request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response get(String url) throws URISyntaxException {
        return get(new URI(url));
    }

    /**
     * Makes a simple HTTP GET request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response get(URI uri) {
        return get(new SimpleRequestOptions(uri));
    }

    /**
     * Makes a simple HTTP GET request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response get(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.GET);
    }

    /**
     * Makes a simple HTTP HEAD request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response head(String url) throws URISyntaxException {
        return head(new URI(url));
    }

    /**
     * Makes a simple HTTP HEAD request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response head(URI uri) {
        return head(new SimpleRequestOptions(uri));
    }

    /**
     * Makes a simple HTTP HEAD request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response head(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.HEAD);
    }


    /**
     * Makes a simple HTTP POST request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response post(String url) throws URISyntaxException {
        return post(new URI(url));
    }

    /**
     * Makes a simple HTTP POST request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response post(URI uri) { return post(new SimpleRequestOptions(uri)); }

    /**
     * Makes a simple HTTP POST request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response post(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.POST);
    }

    /**
     * Makes a simple HTTP PUT request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response put(String url) throws URISyntaxException {
        return put(new URI(url));
    }

    /**
     * Makes a simple HTTP PUT request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response put(URI uri) { return put(new SimpleRequestOptions(uri)); }

    /**
     * Makes a simple HTTP PUT request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response put(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.PUT);
    }

    /**
     * Makes a simple HTTP DELETE request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response delete(String url) throws URISyntaxException {
        return delete(new URI(url));
    }

    /**
     * Makes a simple HTTP DELETE request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response delete(URI uri) { return delete(new SimpleRequestOptions(uri)); }

    /**
     * Makes a simple HTTP DELETE request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response delete(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.DELETE);
    }

    /**
     * Makes a simple HTTP TRACE request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response trace(String url) throws URISyntaxException {
        return trace(new URI(url));
    }

    /**
     * Makes a simple HTTP TRACE request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response trace(URI uri) { return trace(new SimpleRequestOptions(uri)); }

    /**
     * Makes a simple HTTP TRACE request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response trace(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.TRACE);
    }

    /**
     * Makes a simple HTTP OPTIONS request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response options(String url) throws URISyntaxException {
        return options(new URI(url));
    }

    /**
     * Makes a simple HTTP OPTIONS request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response options(URI uri) { return options(new SimpleRequestOptions(uri)); }

    /**
     * Makes a simple HTTP OPTIONS request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response options(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.OPTIONS);
    }

    /**
     * Makes a simple HTTP PATCH request
     * @param url The URL against which to make the request
     * @return The HTTP Response corresponding to the request
     * @throws URISyntaxException
     */
    public static Response patch(String url) throws URISyntaxException {
        return patch(new URI(url));
    }

    /**
     * Makes a simple HTTP PATCH request
     * @param uri The URI against which to make the request
     * @return The HTTP Response corresponding to the request
     */
    public static Response patch(URI uri) { return patch(new SimpleRequestOptions(uri)); }

    /**
     * Makes a simple HTTP PATCH request
     * @param simpleRequestOptions The options to configure the request and the client making it
     * @return The HTTP response corresponding to the request
     */
    public static Response patch(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.PATCH);
    }
}
