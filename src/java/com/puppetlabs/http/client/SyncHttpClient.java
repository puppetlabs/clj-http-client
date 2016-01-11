package com.puppetlabs.http.client;

import com.codahale.metrics.Timer;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.SortedMap;
/**
 * This interface represents a synchronous HTTP client with which
 * requests can be made. An object implementing this interface is
 * returned by {@link com.puppetlabs.http.client.Sync#createClient(ClientOptions)}
 */
public interface SyncHttpClient extends Closeable {

    /**
     * @return a SortedMap of metric name to Timer instance
     */
    public SortedMap<String, Timer> getClientMetrics();

    /**
     * Makes a configurable HTTP request
     * @param requestOptions the options to configure the request
     * @param method the type of the HTTP request
     * @return the HTTP response
     */
    public Response request(RequestOptions requestOptions, HttpMethod method);

    /**
     * Makes an HTTP GET request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response get(String url) throws URISyntaxException;

    /**
     * Makes an HTTP GET request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response get(URI uri);

    /**
     * Makes an HTTP GET request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response get(RequestOptions requestOptions);

    /**
     * Makes an HTTP HEAD request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response head(String url) throws URISyntaxException;

    /**
     * Makes an HTTP HEAD request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response head(URI uri);

    /**
     * Makes an HTTP HEAD request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response head(RequestOptions requestOptions);

    /**
     * Makes an HTTP POST request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response post(String url) throws URISyntaxException;

    /**
     * Makes an HTTP POST request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response post(URI uri);

    /**
     * Makes an HTTP POST request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response post(RequestOptions requestOptions);

    /**
     * Makes an HTTP PUT request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response put(String url) throws URISyntaxException;

    /**
     * Makes an HTTP PUT request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response put(URI uri);

    /**
     * Makes an HTTP PUT request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response put(RequestOptions requestOptions);

    /**
     * Makes an HTTP DELETE request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response delete(String url) throws URISyntaxException;

    /**
     * Makes an HTTP DELETE request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response delete(URI uri);

    /**
     * Makes an HTTP DELETE request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response delete(RequestOptions requestOptions);

    /**
     * Makes an HTTP TRACE request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response trace(String url) throws URISyntaxException;

    /**
     * Makes an HTTP TRACE request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response trace(URI uri);

    /**
     * Makes an HTTP TRACE request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response trace(RequestOptions requestOptions);

    /**
     * Makes an HTTP OPTIONS request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response options(String url) throws URISyntaxException;

    /**
     * Makes an HTTP OPTIONS request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response options(URI uri);

    /**
     * Makes an HTTP OPTIONS request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response options(RequestOptions requestOptions);

    /**
     * Makes an HTTP PATCH request
     * @param url the url against which to make the request
     * @return the HTTP response
     * @throws URISyntaxException
     */
    public Response patch(String url) throws URISyntaxException;

    /**
     * Makes an HTTP PATCH request
     * @param uri the uri against which to make the request
     * @return the HTTP response
     */
    public Response patch(URI uri);

    /**
     * Makes an HTTP PATCH request
     * @param requestOptions the options to configure the request
     * @return the HTTP response
     */
    public Response patch(RequestOptions requestOptions);
}
