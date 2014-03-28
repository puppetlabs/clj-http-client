package com.puppetlabs.http.client.impl;

import org.httpkit.HttpMethod;
import org.httpkit.client.*;

import javax.net.ssl.SSLEngine;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class JavaClient {

    private static HttpClient defaultClient = null;

    private static HttpClient getDefaultClient() throws IOException {
        if (defaultClient == null) {
            defaultClient = new HttpClient();
        }
        return defaultClient;
    }

    private static String buildQueryString(Map<String, String> params) {
        // TODO: add support for nested query params.  For now we assume a flat,
        // String->String data structure.
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "utf8"));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), "utf8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Error while url-encoding query string", e);
            }
        }
        return sb.toString();
    }

    private static String getBasicAuthValue(BasicAuth auth) {
        String userPasswordStr = auth.getUser() + ":" + auth.getPassword();
        try {
            return "Basic " + DatatypeConverter.printBase64Binary(userPasswordStr.getBytes("utf8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error while attmempting to encode basic auth", e);
        }
    }

    private static Map<String, Object> prepareHeaders(RequestOptions options) {
        Map<String, Object> result;
        if (options.getHeaders() != null) {
            result = (Map<String, Object>) options.getHeaders().clone();
        } else {
            result = new HashMap<String, Object>();
        }

        if (options.getFormParams() != null) {
            result.put("Content-Type", "application/x-www-form-urlencoded");
        }
        if (options.getBasicAuth() != null) {
            result.put("Authorization", getBasicAuthValue(options.getBasicAuth()));
        }
        if (options.getOAuthToken() != null) {
            result.put("Authorization", "Bearer " + options.getOAuthToken());
        }
        if (options.getUserAgent() != null) {
            result.put("User-Agent", options.getUserAgent());
        }
        return result;
    }

    private static CoercedRequestOptions coerceRequestOptions(RequestOptions options) throws IOException {
        String url;
        if (options.getQueryParams() != null) {
            if (options.getUrl().indexOf('?') == -1) {
                url = options.getUrl() + "?" + buildQueryString(options.getQueryParams());
            } else {
                url = options.getUrl() + "&" + buildQueryString(options.getQueryParams());
            }
        } else {
            url = options.getUrl();
        }

        SSLEngine sslEngine = null;
        if (options.getSslEngine() != null) {
            sslEngine = options.getSslEngine();
        } else if (options.getInsecure()) {
            sslEngine = SslContextFactory.trustAnybody();
        }

        HttpMethod method = options.getMethod();
        if (method == null) {
            method = HttpMethod.GET;
        }

        Map<String, Object> headers = prepareHeaders(options);

        Object body;
        if (options.getFormParams() != null) {
            body = buildQueryString(options.getFormParams());
        } else {
            body = options.getBody();
        }

        if (options.getMultipartEntities() != null) {
            String boundary = MultipartEntity.genBoundary(options.getMultipartEntities());

            headers = options.getHeaders();
            headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);

            body = MultipartEntity.encode(boundary, options.getMultipartEntities());
        }

        return new CoercedRequestOptions(url, method, headers, body, sslEngine);
    }

    public static Promise<HttpResponse> request(RequestOptions options, IResponseCallback callback)
            throws IOException {
        HttpClient client = options.getClient();
        if (client == null) {
            client = getDefaultClient();
        }

        CoercedRequestOptions coercedOptions = coerceRequestOptions(options);

        RequestConfig config = new RequestConfig(coercedOptions.getMethod(),
                coercedOptions.getHeaders(), coercedOptions.getBody(),
                options.getTimeout(), options.getKeepalive());

        RespListener listener = new RespListener(
                new ResponseHandler(options, coercedOptions, callback), options.getFilter(),
                options.getWorkerPool(), options.getAs().getValue());

        client.exec(options.getUrl(), config, coercedOptions.getSslEngine(), listener);

        return options.getPromise();
    }
}
