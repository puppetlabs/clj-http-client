package com.puppetlabs.http.client.impl;

public enum ResponseBodyType {
    AUTO(1),
    TEXT(2),
    STREAM(3),
    BYTE_ARRAY(4);

    private int value;
    ResponseBodyType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }


}
