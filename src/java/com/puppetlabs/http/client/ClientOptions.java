package com.puppetlabs.http.client;

import com.codahale.metrics.MetricRegistry;

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
    private int connectTimeoutMilliseconds = -1;
    private int socketTimeoutMilliseconds = -1;
    private MetricRegistry metricRegistry;
    private String metricPrefix;
    private String serverId;
    private boolean useURLMetrics = true;

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
     * @param connectTimeoutMilliseconds Maximum number of milliseconds that
     *                                     the client will wait for a
     *                                     connection to be established.  A
     *                                     value of zero is interpreted as
     *                                     infinite.  A negative value is
     *                                     interpreted as undefined (system
     *                                     default).
     * @param socketTimeoutMilliseconds Maximum number of milliseconds that
     *                                    the client will allow for no data to
     *                                    be available on the socket before
     *                                    closing the underlying connection,
     *                                    <code>SO_TIMEOUT</code> in socket
     *                                    terms.  A timeout of zero is
     *                                    interpreted as an infinite timeout.
     *                                    A negative value is interpreted as
     *                                    undefined (system default).
     */
    public ClientOptions(SSLContext sslContext,
                         String sslCert,
                         String sslKey,
                         String sslCaCert,
                         String[] sslProtocols,
                         String[] sslCipherSuites,
                         boolean insecure,
                         boolean forceRedirects,
                         boolean followRedirects,
                         int connectTimeoutMilliseconds,
                         int socketTimeoutMilliseconds) {
        this.sslContext = sslContext;
        this.sslCert = sslCert;
        this.sslKey = sslKey;
        this.sslCaCert = sslCaCert;
        this.sslProtocols = sslProtocols;
        this.sslCipherSuites = sslCipherSuites;
        this.insecure = insecure;
        this.forceRedirects = forceRedirects;
        this.followRedirects = followRedirects;
        this.connectTimeoutMilliseconds = connectTimeoutMilliseconds;
        this.socketTimeoutMilliseconds = socketTimeoutMilliseconds;
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

    public int getConnectTimeoutMilliseconds() {
        return connectTimeoutMilliseconds;
    }

    public ClientOptions setConnectTimeoutMilliseconds(
            int connectTimeoutMilliseconds) {
        this.connectTimeoutMilliseconds = connectTimeoutMilliseconds;
        return this;
    }

    public int getSocketTimeoutMilliseconds() {
        return socketTimeoutMilliseconds;
    }

    public ClientOptions setSocketTimeoutMilliseconds(
            int socketTimeoutMilliseconds) {
        this.socketTimeoutMilliseconds = socketTimeoutMilliseconds;
        return this;
    }

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public ClientOptions setMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        return this;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public ClientOptions setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
        return this;
    }

    public String getServerId() {
        return serverId;
    }

    public ClientOptions setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public boolean isUseURLMetrics() {
        return useURLMetrics;
    }

    public ClientOptions setUseURLMetrics(boolean useURLMetrics) {
        this.useURLMetrics = useURLMetrics;
        return this;
    }
}
