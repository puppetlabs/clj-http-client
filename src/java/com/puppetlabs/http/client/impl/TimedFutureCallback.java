package com.puppetlabs.http.client.impl;

import com.codahale.metrics.Timer;
import org.apache.http.concurrent.FutureCallback;

public final class TimedFutureCallback<T> implements FutureCallback<T> {

    private final FutureCallback<T> delegate;

    private final Timer.Context timerContext;

    public TimedFutureCallback(FutureCallback<T> delegate, Timer.Context timerContext) {
        this.delegate = delegate;
        this.timerContext = timerContext;
    }

    public void completed(T result) {
        if (timerContext != null) {
            timerContext.stop();
        }
        delegate.completed(result);
    }

    public void failed(Exception ex) {
        if (timerContext != null) {
            timerContext.stop();
        }
        delegate.failed(ex);
    }

    public void cancelled() {
        if (timerContext != null) {
            timerContext.stop();
        }
        delegate.cancelled();
    }

}
