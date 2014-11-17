package com.puppetlabs.http.client;

import com.puppetlabs.http.client.impl.SslUtils;
import com.puppetlabs.http.client.impl.JavaClient;
import com.puppetlabs.http.client.impl.PersistentAsyncHttpClient;
import com.puppetlabs.http.client.impl.CoercedClientOptions;

public class Async {
    public static AsyncHttpClient createClient(ClientOptions clientOptions) {
        clientOptions = SslUtils.configureSsl(clientOptions);
        CoercedClientOptions coercedClientOptions = JavaClient.coerceClientOptions(clientOptions);
        return new PersistentAsyncHttpClient(JavaClient.createClient(coercedClientOptions));
    }
}
