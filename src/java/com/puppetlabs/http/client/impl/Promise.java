package com.puppetlabs.http.client.impl;

import java.util.concurrent.CountDownLatch;

public class Promise<T> implements Deliverable<T> {
    private final CountDownLatch latch;
    private T value = null;

    public Promise() {
        latch = new CountDownLatch(1);
    }

    public synchronized void deliver(T t) {
        if (value != null) {
            throw new IllegalStateException("Attempting to deliver value to a promise that has already been realized!");
        }
        value = t;
        latch.countDown();
    }

    public T deref() throws InterruptedException {
        latch.await();
        return value;
    }

}
