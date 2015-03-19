package com.puppetlabs.http.client.impl;

import javax.net.ssl.SSLContext;

public class CoercedClientOptions {
    private final SSLContext sslContext;
    private final String[] sslProtocols;
    private final String[] sslCipherSuites;
    private final boolean forceRedirects;
    private final boolean followRedirects;
    private final int connectTimeoutMilliseconds;
    private final int socketTimeoutMilliseconds;

    public CoercedClientOptions(SSLContext sslContext,
                                String[] sslProtocols,
                                String[] sslCipherSuites,
                                boolean forceRedirects,
                                boolean followRedirects,
                                int connectTimeoutMilliseconds,
                                int socketTimeoutMilliseconds) {
        this.sslContext = sslContext;
        this.sslProtocols = sslProtocols;
        this.sslCipherSuites = sslCipherSuites;
        this.forceRedirects = forceRedirects;
        this.followRedirects = followRedirects;
        this.connectTimeoutMilliseconds = connectTimeoutMilliseconds;
        this.socketTimeoutMilliseconds = socketTimeoutMilliseconds;
    }

    public SSLContext getSslContext() { return sslContext; }

    public String[] getSslProtocols() { return sslProtocols; }

    public String[] getSslCipherSuites() { return sslCipherSuites; }

    public boolean getForceRedirects() { return forceRedirects; }

    public boolean getFollowRedirects() { return followRedirects; }

    public int getConnectTimeoutMilliseconds() {
        return connectTimeoutMilliseconds;
    }

    public int getSocketTimeoutMilliseconds() {
        return socketTimeoutMilliseconds;
    }
}
