package com.puppetlabs.http.client.impl;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

// To use this class call SafeRedirectedRequest.wrap(HttpUriRequest). Will
// wrap the given request and proxy all methods to it, except for setHeaders,
// which we implement and filter out potentially unsafe headers from.
//
// See https://stackoverflow.com/questions/30344715/automatically-delegating-all-methods-of-a-java-class
// for inspiration for this work.
public class SafeRedirectedRequest
        // We extend HttpGet to satisfy the requirement to have implementations
        // of the HttpUriRequest, but proxy all invocations to the wrapped delegate.
        extends HttpGet
        implements HttpUriRequest, InvocationHandler {

    private final String AUTH_HEADER = "Authorization";
    private final String XAUTH_HEADER = "X-Authorization";

    private final HttpUriRequest delegate;

    public SafeRedirectedRequest(HttpUriRequest delegate) {
        this.delegate = delegate;
    }

    public static HttpUriRequest wrap(HttpUriRequest wrapped) {
        return (HttpUriRequest) Proxy.newProxyInstance(HttpUriRequest.class.getClassLoader(),
                new Class[]{HttpUriRequest.class},
                new SafeRedirectedRequest(wrapped));
    }

    // There are other ways to set headers (setHeader(String, String),
    // setHeader(Header)), however this is the method currently used to
    // copy exiting headers when being redirected.
    @Override
    public void setHeaders(final Header[] headers) {
        final Header[] cleanedHeaders = (Header[]) Arrays.asList(headers)
                .stream()
                .filter(header -> AUTH_HEADER.equalsIgnoreCase(header.getName()))
                .filter(header -> XAUTH_HEADER.equalsIgnoreCase(header.getName()))
                .toArray(Header[]::new);
        delegate.setHeaders(cleanedHeaders);
    }

    // Begin Proxy/InvocationHandler implementation
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = findMethod(this.getClass(), method);
        if (m != null) {
            return m.invoke(this, args);
        } else {
            return method.invoke(this.delegate, args);
        }
    }

    private Method findMethod(Class<?> clazz, Method method) throws Throwable {
        try {
            return clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
