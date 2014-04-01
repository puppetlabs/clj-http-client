package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.HttpResponse;

public interface IResponseCallback {
    HttpResponse handleResponse(HttpResponse response);
}
