package com.puppetlabs.http.client.impl;

import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

// This class overrides the getRedirect() method of DefaultRedirectStrategy
// (or LaxRedirectStrategy as it inherits this method from DefaultRedirectStrategy)
// so that we do not copy the auth headers for non get or head requests, and the newly
// created request is wrapped in a SafeRedirectedRequest. SafeRedirectedRequest
// will proxy method invocations to the wrapped request, but intercepts setHeaders(),
// which is used by callers to copy over headers from the original request.
//
// See https://stackoverflow.com/questions/17970633/header-values-overwritten-on-redirect-in-httpclient
// for the inspiration for this work.
//
// Note: This implementation is VERY version specific and will need to be
// revised whenever updating httpclient.
public class CreateRedirectUtil {
    public static final int SC_PERMANENT_REDIRECT = 308;
    public static final List<String> SECURITY_RELATED_HEADERS = Arrays.asList(
            "X-Authorization", "Authorization",
            "Cookie", "Set-Cookie", "WWW-Authenticate",
            "Proxy-Authorization", "Proxy-Authenticate"
    );

    public static HttpUriRequest getRedirect(
            final DefaultRedirectStrategy redirectStrategy,
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws ProtocolException {

        final URI uri = redirectStrategy.getLocationURI(request, response, context);
        final String method = request.getRequestLine().getMethod();

        // This is new to allow Redirects to contain Auth headers IF they are to the same host/port/scheme
        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final HttpHost target = clientContext.getTargetHost();
        final boolean localRedirect = target.getHostName().equals(uri.getHost()) &&
                target.getPort() == uri.getPort() &&
                target.getSchemeName().equals(uri.getScheme());


        if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
            return localRedirect ? new HttpHead(uri) : SafeRedirectedRequest.wrap(new HttpHead(uri));
        } else if (method.equalsIgnoreCase(HttpGet.METHOD_NAME)) {
            return localRedirect ? new HttpGet(uri) : SafeRedirectedRequest.wrap(new HttpGet(uri));
        } else {
            final int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_TEMPORARY_REDIRECT || status == SC_PERMANENT_REDIRECT) {

                if (! localRedirect) {
                    // RequestBuilder.copy will copy any existing headers, which we don't want
                    RequestBuilder builder = RequestBuilder.copy(request).setUri(uri);
                    for (String header : SECURITY_RELATED_HEADERS) {
                        // .removeHeaders does an equalsIgnoreCase() on the passed String.
                        builder.removeHeaders(header);
                    }

                    return SafeRedirectedRequest.wrap(builder.build());
                } else {
                    return RequestBuilder.copy(request).setUri(uri).build();
                }
            } else {
                return localRedirect ? new HttpGet(uri) : SafeRedirectedRequest.wrap(new HttpGet(uri));
            }
        }
    }

}
