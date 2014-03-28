package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.HttpResponse;
import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.Promise;
import com.puppetlabs.http.client.impl.SslUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class SyncHttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncHttpClient.class);


    private static void logAndRethrow(String msg, Throwable t) {
        LOGGER.error(msg, t);
        throw new RuntimeException(msg, t);
    }

    private static RequestOptions configureSslFromContext(RequestOptions options) {
        options.setSslEngine(options.getSslContext().createSSLEngine());
        options.setSslContext(null);
        return options;
    }

    // TODO: move this into the async java API if we ever add one
    private static RequestOptions configureSsl(RequestOptions options) {
        if (options.getSslEngine() != null) {
            return options;
        }

        if (options.getSslContext() != null) {
            return configureSslFromContext(options);
        }

        if ((options.getSslCert() != null) &&
                (options.getSslKey() != null) &&
                (options.getSslCaCert() != null)) {
            try {
                options.setSslContext(
                        SslUtils.pemsToSSLContext(
                                new FileReader(options.getSslCert()),
                                new FileReader(options.getSslKey()),
                                new FileReader(options.getSslCaCert()))
                );
            } catch (KeyStoreException e) {
                logAndRethrow("Error while configuring SSL", e);
            } catch (CertificateException e) {
                logAndRethrow("Error while configuring SSL", e);
            } catch (IOException e) {
                logAndRethrow("Error while configuring SSL", e);
            } catch (NoSuchAlgorithmException e) {
                logAndRethrow("Error while configuring SSL", e);
            } catch (KeyManagementException e) {
                logAndRethrow("Error while configuring SSL", e);
            } catch (UnrecoverableKeyException e) {
                logAndRethrow("Error while configuring SSL", e);
            }
            options.setSslCert(null);
            options.setSslKey(null);
            options.setSslCaCert(null);
            return configureSslFromContext(options);
        }

        return options;
    }

    public static HttpResponse request(RequestOptions options) {
        // TODO: if we end up implementing an async version of the java API,
        // we should refactor this implementation so that it is based on the
        // async one, as Patrick has done in the clojure API.

        options = configureSsl(options);

        Promise<HttpResponse> promise = null;
        try {
            promise = JavaClient.request(options, null);
        } catch (IOException e) {
            logAndRethrow("Error submitting http request", e);

        }
        HttpResponse response = null;
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


    public static HttpResponse get(String url) {
        return get(new RequestOptions(url));
    }

    public static HttpResponse get(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET));
    }

    public static HttpResponse post(String url) {
        return post(new RequestOptions(url));
    }

    public static HttpResponse post(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.POST));
    }
}
