package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.SslUtils;
import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.PersistentSyncHttpClient;
import com.puppetlabs.http.client.impl.CoercedClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

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
        return new RequestOptions(uri, headers, body, decompressBody, as);
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

        return new ClientOptions(sslContext, sslCert, sslKey, sslCaCert,
                sslProtocols, sslCipherSuites, insecure,
                forceRedirects, followRedirects);
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

    public static SyncHttpClient createClient(ClientOptions clientOptions) {
        CoercedClientOptions coercedClientOptions =
                JavaClient.coerceClientOptions(
                        SslUtils.configureSsl(clientOptions));
        return new PersistentSyncHttpClient(
                JavaClient.createClient(coercedClientOptions));
    }

    public static Response get(String url) throws URISyntaxException {
        return get(new URI(url));
    }
    public static Response get(URI uri) {
        return get(new SimpleRequestOptions(uri));
    }
    public static Response get(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.GET);
    }

    public static Response head(String url) throws URISyntaxException {
        return head(new URI(url));
    }
    public static Response head(URI uri) {
        return head(new SimpleRequestOptions(uri));
    }
    public static Response head(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.HEAD);
    }

    public static Response post(String url) throws URISyntaxException {
        return post(new URI(url));
    }
    public static Response post(URI uri) { return post(new SimpleRequestOptions(uri)); }
    public static Response post(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.POST);
    }

    public static Response put(String url) throws URISyntaxException {
        return put(new URI(url));
    }
    public static Response put(URI uri) { return put(new SimpleRequestOptions(uri)); }
    public static Response put(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.PUT);
    }

    public static Response delete(String url) throws URISyntaxException {
        return delete(new URI(url));
    }
    public static Response delete(URI uri) { return delete(new SimpleRequestOptions(uri)); }
    public static Response delete(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.DELETE);
    }

    public static Response trace(String url) throws URISyntaxException {
        return trace(new URI(url));
    }
    public static Response trace(URI uri) { return trace(new SimpleRequestOptions(uri)); }
    public static Response trace(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.TRACE);
    }

    public static Response options(String url) throws URISyntaxException {
        return options(new URI(url));
    }
    public static Response options(URI uri) { return options(new SimpleRequestOptions(uri)); }
    public static Response options(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.OPTIONS);
    }

    public static Response patch(String url) throws URISyntaxException {
        return patch(new URI(url));
    }
    public static Response patch(URI uri) { return patch(new SimpleRequestOptions(uri)); }
    public static Response patch(SimpleRequestOptions simpleRequestOptions) {
        return request(simpleRequestOptions, HttpMethod.PATCH);
    }
}
