package com.puppetlabs.http.client.impl;

import org.httpkit.HttpMethod;
import org.httpkit.HttpUtils;
import org.httpkit.client.IResponseHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResponseHandler implements IResponseHandler {

    private static final Set<Integer> REDIRECT_STATUS_CODES =
            new HashSet<Integer>(Arrays.asList(301, 302, 303, 307, 308));

    private final RequestOptions options;
    private final CoercedRequestOptions coercedOptions;
    private final IResponseCallback callback;

    public ResponseHandler(RequestOptions options,
                           CoercedRequestOptions coercedOptions,
                           IResponseCallback callback) {
        this.options = options;
        this.coercedOptions = coercedOptions;
        this.callback = callback;
    }

    private HttpMethod getNewMethod(int status) {
        if (status == 301 || status == 302 || status == 303) {
            return HttpMethod.GET;
        } else {
            return options.getMethod();
        }
    }

    private void deliverResponse(HttpResponse response) {
        HttpResponse finalResponse = response;
        try {
            if (callback != null) {
                finalResponse = callback.handleResponse(response);
            }
        } catch (Exception e) {
            // dump stacktrace to stderr
            HttpUtils.printError(coercedOptions.getMethod() + " " +
                    coercedOptions.getUrl() + "'s callback", e);
            // return the error
            options.getPromise().deliver(new HttpResponse(options, e));
        }
        options.getPromise().deliver(finalResponse);
    }

    @Override
    public void onSuccess(int status, Map<String, Object> headers, Object body) {
        if (options.getFollowRedirects() && REDIRECT_STATUS_CODES.contains(status)) {
            if (options.getMaxRedirects() >= options.getTraceRedirects().size()) {
                // follow 301 and 302 redirect
                try {
                    JavaClient.request(
                            options.setUrl(new URI(coercedOptions.getUrl()).resolve((String) headers.get("location")).toString())
                                    .setMethod(getNewMethod(status))
                                    .addTraceRedirect(coercedOptions.getUrl()),
                            callback);
                } catch (IOException e) {
                    throw new RuntimeException("Error when attempting redirect", e);
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Error when attempting redirect", e);
                }
            } else {
                deliverResponse(new HttpResponse(options,
                        new Exception("too many redirects: " + options.getTraceRedirects().size())));
            }
        } else {
            deliverResponse(new HttpResponse(options, body, headers, status));
        }
    }


    @Override
    public void onThrowable(Throwable t) {
        deliverResponse(new HttpResponse(options, t));
    }
}
