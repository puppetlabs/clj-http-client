package com.puppetlabs.http.client.impl;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

import java.net.URI;

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

    public static HttpUriRequest getRedirect(
            final DefaultRedirectStrategy redirectStrategy,
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws ProtocolException {

        final URI uri = redirectStrategy.getLocationURI(request, response, context);
        final String method = request.getRequestLine().getMethod();

        if (method.equalsIgnoreCase(HttpHead.METHOD_NAME)) {
            return SafeRedirectedRequest.wrap(new HttpHead(uri));
        } else if (method.equalsIgnoreCase(HttpGet.METHOD_NAME)) {
            return SafeRedirectedRequest.wrap(new HttpGet(uri));
        } else {
            final int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_TEMPORARY_REDIRECT || status == SC_PERMANENT_REDIRECT) {

                // RequestBuilder.copy will copy any existing headers, which we don't want
                // .removeHeaders does an equalsIgnoreCase() on the passed String.
                HttpUriRequest copiedReq = RequestBuilder.copy(request)
                        .setUri(uri)
                        .removeHeaders("authorization")
                        .removeHeaders("x-authorization")
                        .build();

                return SafeRedirectedRequest.wrap(copiedReq);
            } else {
                return SafeRedirectedRequest.wrap(new HttpGet(uri));
            }
        }
    }

}
