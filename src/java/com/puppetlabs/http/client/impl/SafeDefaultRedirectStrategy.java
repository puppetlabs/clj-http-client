package com.puppetlabs.http.client.impl;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

// Shares behavior of CreateRedirectUtil.getRedirected() with SafeLaxRedirectStrategy
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class SafeDefaultRedirectStrategy extends DefaultRedirectStrategy {

    // Used by the Apache HttpClientBuilder internally
    public static final SafeDefaultRedirectStrategy INSTANCE = new SafeDefaultRedirectStrategy();

    @Override
    public HttpUriRequest getRedirect(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws ProtocolException {

        return CreateRedirectUtil.getRedirect(this, request, response, context);
    }
}
