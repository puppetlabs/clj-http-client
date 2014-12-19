package com.puppetlabs.http.client;

import javax.net.ssl.SSLContext;

/**
 * This class is a wrapper around a number of options for use
 * in configuring a persistent HTTP Client.
 *
 * @see com.puppetlabs.http.client.Async#createClient(ClientOptions)
 * @see com.puppetlabs.http.client.Sync#createClient(ClientOptions)
 */
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

    /**
     * Constructor for the ClientOptions class. When this constructor is called,
     * insecure and forceRedirects will default to false, and followRedirects will default
     * to true.
     */
    public ClientOptions() { }

    /**
     * Constructor for the ClientOptions class.
     * @param sslContext The SSL context for the client.
     * @param sslCert The path to a PEM file containing the client cert
     * @param sslKey The path to a PEM file containing the client private key
     * @param sslCaCert The path to a PEM file containing the CA cert
     * @param sslProtocols The SSL protocols that the client can select from when talking to the server
     * @param sslCipherSuites The cipher suites that the client can select from when talking to the server
     * @param insecure Whether or not the client should use an insecure connection.
     * @param forceRedirects Whether or not the client should follow redirects on POST or PUT requests.
     * @param followRedirects Whether or not the client should follow redirects in general.
     */
    public ClientOptions(SSLContext sslContext,
                         String sslCert,
                         String sslKey,
                         String sslCaCert,
                         String[] sslProtocols,
                         String[] sslCipherSuites,
                         boolean insecure,
                         boolean forceRedirects,
                         boolean followRedirects) {
        this.sslContext = sslContext;
        this.sslCert = sslCert;
        this.sslKey = sslKey;
        this.sslCaCert = sslCaCert;
        this.sslProtocols = sslProtocols;
        this.sslCipherSuites = sslCipherSuites;
        this.insecure = insecure;
        this.forceRedirects = forceRedirects;
        this.followRedirects = followRedirects;
    }

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
