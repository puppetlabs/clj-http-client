package com.puppetlabs.http.client.impl;

import com.puppetlabs.http.client.HttpClientException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class Compression {

    public static InputStream gunzip(InputStream gzipped) {
        try {
            return new GZIPInputStream(gzipped);
        } catch (IOException e) {
            throw new HttpClientException("Unable to gunzip stream", e);
        }
    }

    public static InputStream inflate(InputStream deflated) {
        return new InflaterInputStream(deflated);
    }
}
