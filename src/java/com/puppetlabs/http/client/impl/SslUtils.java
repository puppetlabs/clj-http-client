package com.puppetlabs.http.client.impl;

import com.puppetlabs.ssl_utils.SSLUtils;
import com.puppetlabs.http.client.HttpClientException;
import com.puppetlabs.http.client.ClientOptions;
import com.puppetlabs.http.client.Sync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class SslUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SslUtils.class);

    public static ClientOptions configureSsl(ClientOptions options) {
        if (options.getSslContext() != null) {
            return options;
        }

        if ((options.getSslCert() != null) &&
                (options.getSslKey() != null) &&
                (options.getSslCaCert() != null)) {
            try {
                options.setSslContext(
                        SSLUtils.pemsToSSLContext(
                                new FileReader(options.getSslCert()),
                                new FileReader(options.getSslKey()),
                                new FileReader(options.getSslCaCert()))
                );
            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException |
                    KeyManagementException | UnrecoverableKeyException | NoSuchProviderException e) {
                LOGGER.error("Error while configuring SSL", e);
                throw new HttpClientException("Error while configuring SSL", e);
            }
            options.setSslCert(null);
            options.setSslKey(null);
            options.setSslCaCert(null);
            return options;
        }

        if (options.getSslCaCert() != null) {
            try {
                options.setSslContext(
                        SSLUtils.caCertPemToSSLContext(
                                new FileReader(options.getSslCaCert()))
                );
            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException |
                    KeyManagementException | NoSuchProviderException e) {
                LOGGER.error("Error while configuring SSL", e);
                throw new HttpClientException("Error while configuring SSL", e);
            }
            options.setSslCaCert(null);
            return options;
        }

        return options;
    }
}
