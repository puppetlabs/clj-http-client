package com.puppetlabs.http.client.impl;

import clojure.lang.IFn;

public class FnDeliverable<T> implements Deliverable<T> {

    private final IFn fn;

    public FnDeliverable(IFn fn) {
        this.fn = fn;
    }

    @Override
    public void deliver(T t) {
        fn.invoke(t);
    }
}
