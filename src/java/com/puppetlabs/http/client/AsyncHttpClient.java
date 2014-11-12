package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.Promise;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;

public interface AsyncHttpClient extends Closeable{
    public Promise<Response> get(String url) throws URISyntaxException;
    public Promise<Response> get(URI uri);
    public Promise<Response> get(RequestOptions requestOptions);

    public Promise<Response> head(String url) throws URISyntaxException;
    public Promise<Response> head(URI uri);
    public Promise<Response> head(RequestOptions requestOptions);

    public Promise<Response> post(String url) throws URISyntaxException;
    public Promise<Response> post(URI uri);
    public Promise<Response> post(RequestOptions requestOptions);

    public Promise<Response> put(String url) throws URISyntaxException;
    public Promise<Response> put(URI uri);
    public Promise<Response> put(RequestOptions requestOptions);

    public Promise<Response> delete(String url) throws URISyntaxException;
    public Promise<Response> delete(URI uri);
    public Promise<Response> delete(RequestOptions requestOptions);

    public Promise<Response> trace(String url) throws URISyntaxException;
    public Promise<Response> trace(URI uri);
    public Promise<Response> trace(RequestOptions requestOptions);

    public Promise<Response> options(String url) throws URISyntaxException;
    public Promise<Response> options(URI uri);
    public Promise<Response> options(RequestOptions requestOptions);

    public Promise<Response> patch(String url) throws URISyntaxException;
    public Promise<Response> patch(URI uri);
    public Promise<Response> patch(RequestOptions requestOptions);
}
