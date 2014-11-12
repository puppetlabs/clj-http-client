package com.puppetlabs.http.client;

import java.net.URI;
import java.net.URISyntaxException;

public interface SyncHttpClient {
    public void close();

    public Response get(String url) throws URISyntaxException;
    public Response get(URI uri);
    public Response get(RequestOptions requestOptions);

    public Response head(String url) throws URISyntaxException;
    public Response head(URI uri);
    public Response head(RequestOptions requestOptions);

    public Response post(String url) throws URISyntaxException;
    public Response post(URI uri);
    public Response post(RequestOptions requestOptions);

    public Response put(String url) throws URISyntaxException;
    public Response put(URI uri);
    public Response put(RequestOptions requestOptions);

    public Response delete(String url) throws URISyntaxException;
    public Response delete(URI uri);
    public Response delete(RequestOptions requestOptions);

    public Response trace(String url) throws URISyntaxException;
    public Response trace(URI uri);
    public Response trace(RequestOptions requestOptions);

    public Response options(String url) throws URISyntaxException;
    public Response options(URI uri);
    public Response options(RequestOptions requestOptions);

    public Response patch(String url) throws URISyntaxException;
    public Response patch(URI uri);
    public Response patch(RequestOptions requestOptions);
}
