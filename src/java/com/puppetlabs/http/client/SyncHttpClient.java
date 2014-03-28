package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.HttpResponse;
import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.Promise;
import com.puppetlabs.http.client.impl.RequestOptions;
import org.httpkit.HttpMethod;

import java.io.IOException;

public class SyncHttpClient {
    public static HttpResponse request(RequestOptions options) {
        Promise<HttpResponse> promise = null;
        try {
            promise = JavaClient.request(options, null);
        } catch (IOException e) {
            throw new RuntimeException("Error submitting http request", e);
        }
        HttpResponse response = null;
        try {
            response = promise.deref();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while waiting for http response", e);
        }
        if (response.getError() != null) {
            throw new RuntimeException("Error in http request", response.getError());
        }
        return response;
    }

    public static HttpResponse get(String url) {
        return get(new RequestOptions(url));
    }

    private static HttpResponse get(RequestOptions requestOptions) {
        return request(requestOptions.setMethod(HttpMethod.GET));
    }
}
