package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.Response;

public interface IResponseCallback {
    Response handleResponse(Response response);
}
