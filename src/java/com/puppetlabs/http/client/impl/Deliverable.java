package com.puppetlabs.http.client.impl;

public interface Deliverable<T> {
    void deliver(T t);
}
