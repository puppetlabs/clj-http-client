package com.puppetlabs.http.client;

import com.codahale.metrics.Timer;
import com.puppetlabs.http.client.impl.Promise;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.SortedMap;

/**
 * This interface represents an asynchronous HTTP client with which
 * requests can be made. An object implementing this interface is returned by
 * {@link com.puppetlabs.http.client.Async#createClient(ClientOptions)}.
 */
public interface AsyncHttpClient extends Closeable{

    /**
     * @return a SortedMap of metric name to Timer instance
     */
    public SortedMap<String, Timer> getClientMetrics();

    /**
     * Performs a GET request
     * @param url the URL against which to make the GET request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> get(String url) throws URISyntaxException;

    /**
     * Performs a GET request
     * @param uri the URI against which to make the GET request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> get(URI uri);

    /**
     * Performs a GET request
     * @param requestOptions options to configure the GET request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> get(RequestOptions requestOptions);

    /**
     * Performs a HEAD request
     * @param url the URL against which to make the HEAD request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> head(String url) throws URISyntaxException;

    /**
     * Performs a HEAD request
     * @param uri the URI against which to make the HEAD request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> head(URI uri);

    /**
     * Performs a HEAD request
     * @param requestOptions options to configure the HEAD request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> head(RequestOptions requestOptions);

    /**
     * Performs a POST request
     * @param url the URL against which to make the POST request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> post(String url) throws URISyntaxException;

    /**
     * Performs a POST request
     * @param uri the URI against which to make the POST request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> post(URI uri);

    /**
     * Performs a POST request
     * @param requestOptions options to configure the POST request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> post(RequestOptions requestOptions);

    /**
     * Performs a PUT request
     * @param url the URL against which to make the PUT request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> put(String url) throws URISyntaxException;

    /**
     * Performs a PUT request
     * @param uri the URI against which to make the PUT request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> put(URI uri);

    /**
     * Performs a PUT request
     * @param requestOptions options to configure the PUT request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> put(RequestOptions requestOptions);

    /**
     * Performs a DELETE request
     * @param url the URL against which to make the DELETE request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> delete(String url) throws URISyntaxException;

    /**
     * Performs a DELETE request
     * @param uri the URI against which to make the DELETE request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> delete(URI uri);

    /**
     * Performs a DELETE request
     * @param requestOptions options to configure the DELETE request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> delete(RequestOptions requestOptions);

    /**
     * Performs a TRACE request
     * @param url the URL against which to make the TRACE request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> trace(String url) throws URISyntaxException;

    /**
     * Performs a TRACE request
     * @param uri the URI against which to make the TRACE request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> trace(URI uri);

    /**
     * Performs a TRACE request
     * @param requestOptions options to configure the TRACE request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> trace(RequestOptions requestOptions);

    /**
     * Performs an OPTIONS request
     * @param url the URL against which to make the OPTIONS request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> options(String url) throws URISyntaxException;

    /**
     * Performs an OPTIONS request
     * @param uri the URI against which to make the OPTIONS request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> options(URI uri);

    /**
     * Performs an OPTIONS request
     * @param requestOptions options to configure the OPTIONS request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> options(RequestOptions requestOptions);

    /**
     * Performs a PATCH request
     * @param url the URL against which to make the PATCH request
     * @return a Promise with the contents of the response
     * @throws URISyntaxException
     */
    public Promise<Response> patch(String url) throws URISyntaxException;

    /**
     * Performs a PATCH request
     * @param uri the URI against which to make the PATCH request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> patch(URI uri);

    /**
     * Performs a PATCH request
     * @param requestOptions options to configure the PATCH request
     * @return a Promise with the contents of the response
     */
    public Promise<Response> patch(RequestOptions requestOptions);
}
