package com.puppetlabs.http.client;

// This is really dumb, but I didn't want to leak the HTTPKit class into the
// API for now.

public enum HttpMethod {
    GET(org.httpkit.HttpMethod.GET),
    HEAD(org.httpkit.HttpMethod.HEAD),
    POST(org.httpkit.HttpMethod.POST),
    PUT(org.httpkit.HttpMethod.PUT),
    DELETE(org.httpkit.HttpMethod.DELETE),
    TRACE(org.httpkit.HttpMethod.TRACE),
    OPTIONS(org.httpkit.HttpMethod.OPTIONS),
    CONNECT(org.httpkit.HttpMethod.CONNECT),
    PATCH(org.httpkit.HttpMethod.PATCH);


    private org.httpkit.HttpMethod httpKitMethod;

    HttpMethod(org.httpkit.HttpMethod httpKitMethod) {
        this.httpKitMethod = httpKitMethod;
    }

    public org.httpkit.HttpMethod getValue() {
        return this.httpKitMethod;
    }

}
