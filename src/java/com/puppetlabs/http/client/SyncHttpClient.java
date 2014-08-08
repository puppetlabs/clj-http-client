package com.puppetlabs.http.client;

import com.puppetlabs.certificate_authority.CertificateAuthority;
import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.Promise;
import com.puppetlabs.http.client.impl.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class SyncHttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncHttpClient.class);


    private static void logAndRethrow(String msg, Throwable t) {
        LOGGER.error(msg, t);
        throw new HttpClientException(msg, t);
    }

    public static Response request(RequestOptions options) {
        // TODO: if we end up implementing an async version of the java API,
        // we should refactor this implementation so that it is based on the
        // async one, as Patrick has done in the clojure API.

        options = SslUtils.configureSsl(options);

        Promise<Response> promise =  JavaClient.request(options, null);

        Response response = null;
        try {
            response = promise.deref();
        } catch (InterruptedException e) {
            logAndRethrow("Error while waiting for http response", e);
        }
        if (response.getError() != null) {
            logAndRethrow("Error executing http request", response.getError());
        }
        return response;
    }


    public static Response get(String url) throws URISyntaxException {
        return get(new RequestOptions(new URI(url)));
    }
    public static Response get(URI uri) {
        return get(new RequestOptions(uri));
    }
    public static Response get(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET));
    }

    public static Response head(String url) throws URISyntaxException {
        return head(new RequestOptions(new URI(url)));
    }
    public static Response head(URI uri) {
        return head(new RequestOptions(uri));
    }
    public static Response head(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.HEAD));
    }

    public static Response post(String url) throws URISyntaxException {
        return post(new RequestOptions(new URI(url)));
    }
    public static Response post(URI uri) { return post(new RequestOptions(uri)); }
    public static Response post(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.POST));
    }

    public static Response put(String url) throws URISyntaxException {
        return put(new RequestOptions(new URI(url)));
    }
    public static Response put(URI uri) { return put(new RequestOptions(uri)); }
    public static Response put(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PUT));
    }

    public static Response delete(String url) throws URISyntaxException {
        return delete(new RequestOptions(new URI(url)));
    }
    public static Response delete(URI uri) { return delete(new RequestOptions(uri)); }
    public static Response delete(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.DELETE));
    }

    public static Response trace(String url) throws URISyntaxException {
        return trace(new RequestOptions(new URI(url)));
    }
    public static Response trace(URI uri) { return trace(new RequestOptions(uri)); }
    public static Response trace(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.TRACE));
    }

    public static Response options(String url) throws URISyntaxException {
        return options(new RequestOptions(new URI(url)));
    }
    public static Response options(URI uri) { return options(new RequestOptions(uri)); }
    public static Response options(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.OPTIONS));
    }

    public static Response patch(String url) throws URISyntaxException {
        return patch(new RequestOptions(new URI(url)));
    }
    public static Response patch(URI uri) { return patch(new RequestOptions (uri)); }
    public static Response patch(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.PATCH));
    }
}
