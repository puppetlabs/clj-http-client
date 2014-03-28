package com.puppetlabs.http.client.impl;

public interface IResponseCallback {
    HttpResponse handleResponse(HttpResponse response);
}
