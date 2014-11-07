package com.puppetlabs.http.client;


import org.apache.http.nio.client.HttpAsyncClient;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class ClientOptions {
    public static final String[] DEFAULT_SSL_PROTOCOLS =
            new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};

    private SSLContext sslContext;
    private String sslCert;
    private String sslKey;
    private String sslCaCert;
    private String[] sslProtocols;
    private String[] sslCipherSuites;
    private boolean insecure = false;
    private boolean forceRedirects = false;
    private boolean followRedirects = true;

    public ClientOptions() { }

    public SSLContext getSslContext() {
        return sslContext;
    }
    public ClientOptions setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public String getSslCert() {
        return sslCert;
    }
    public ClientOptions setSslCert(String sslCert) {
        this.sslCert = sslCert;
        return this;
    }

    public String getSslKey() {
        return sslKey;
    }
    public ClientOptions setSslKey(String sslKey) {
        this.sslKey = sslKey;
        return this;
    }

    public String getSslCaCert() {
        return sslCaCert;
    }
    public ClientOptions setSslCaCert(String sslCaCert) {
        this.sslCaCert = sslCaCert;
        return this;
    }

    public String[] getSslProtocols() {
        return sslProtocols;
    }
    public ClientOptions setSslProtocols(String[] sslProtocols) {
        this.sslProtocols = sslProtocols;
        return this;
    }

    public String[] getSslCipherSuites() {
        return sslCipherSuites;
    }
    public ClientOptions setSslCipherSuites(String[] sslCipherSuites) {
        this.sslCipherSuites = sslCipherSuites;
        return this;
    }

    public boolean getInsecure() {
        return insecure;
    }
    public ClientOptions setInsecure(boolean insecure) {
        this.insecure = insecure;
        return this;
    }

    public boolean getForceRedirects() { return forceRedirects; }
    public ClientOptions setForceRedirects(boolean forceRedirects) {
        this.forceRedirects = forceRedirects;
        return this;
    }

    public boolean getFollowRedirects() { return followRedirects; }
    public ClientOptions setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }
}
