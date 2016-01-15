package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.RequestOptions;
import org.apache.http.entity.ContentType;

public interface ResponseDeliveryDelegate {

    void deliverResponse(RequestOptions requestOptions,
                         String origContentEncoding,
                         Object body,
                         java.util.Map<String, String> headers,
                         int statusCode,
                         ContentType contentType,
                         IResponseCallback callback);


    void deliverResponse(RequestOptions requestOptions,
                         Exception e);

}
