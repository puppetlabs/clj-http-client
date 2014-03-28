package com.puppetlabs.http.client.impl;

import org.httpkit.client.HttpClient;

import java.io.IOException;

public class DefaultClient {
    private static HttpClient instance;

    public synchronized static HttpClient getInstance() {
        if (instance == null) {
            try {
                instance = new HttpClient();
            } catch (IOException e) {
                throw new RuntimeException("Error attempting to instantiate HttpClient", e);
            }
        }
        return instance;
    }
}
