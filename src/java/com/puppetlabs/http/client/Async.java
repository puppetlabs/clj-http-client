package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.PersistentAsyncHttpClient;
import com.puppetlabs.http.client.metrics.Metrics;

/**
 * This class allows you to create an AsyncHttpClient for use in making
 * HTTP Requests. It consists exclusively of a static method to create
 * a client.
 */
public class Async {

    /**
     * Allows you to create an instance of an AsyncHttpClient for use in
     * making HTTP requests.
     *
     * @param clientOptions the list of options with which to configure the client
     * @return an AsyncHttpClient that can be used to make requests
     */
    public static AsyncHttpClient createClient(ClientOptions clientOptions) {
        final String metricNamespace = Metrics.buildMetricNamespace(clientOptions.getMetricPrefix(),
                clientOptions.getServerId());
        return new PersistentAsyncHttpClient(JavaClient.createClient(clientOptions),
                clientOptions.getMetricRegistry(),metricNamespace);
    }
}
