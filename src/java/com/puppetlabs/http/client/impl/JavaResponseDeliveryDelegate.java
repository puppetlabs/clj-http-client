package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.RequestOptions;
import com.puppetlabs.http.client.Response;
import org.apache.http.entity.ContentType;

import java.util.Map;

public final class JavaResponseDeliveryDelegate implements ResponseDeliveryDelegate {

    private final Promise<Response> promise;

    public JavaResponseDeliveryDelegate(Promise<Response> promise) {
        this.promise = promise;
    }

    private void deliverResponse(Response response, RequestOptions requestOptions, IResponseCallback callback) {
        if (callback != null) {
            try {
                promise.deliver(callback.handleResponse(response));
            } catch (Exception e) {
                promise.deliver(new Response(requestOptions, e));
            }
        } else {
            promise.deliver(response);
        }
    }

    @Override
    public void deliverResponse(RequestOptions requestOptions,
                                String origContentEncoding,
                                Object body,
                                Map<String, String> headers,
                                int statusCode,
                                String reasonPhrase,
                                ContentType contentType,
                                IResponseCallback callback) {
        Response response = new Response(requestOptions,
                origContentEncoding,
                body,
                headers,
                statusCode,
                reasonPhrase,
                contentType);
        deliverResponse(response, requestOptions, callback);
    }

    @Override
    public void deliverResponse(RequestOptions requestOptions,
                                Exception e,
                                IResponseCallback callback) {
        deliverResponse(new Response(requestOptions, e), requestOptions, callback);
    }

}
