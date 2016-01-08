package com.puppetlabs.http.client;

/**
 * This Enum represents the possible types of the body of a response.
 */
public enum ResponseBodyType {
    UNBUFFERED_STREAM,
    STREAM,
    TEXT;
}
