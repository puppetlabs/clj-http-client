package com.puppetlabs.http.client.impl;

import com.codahale.metrics.Timer;
import org.apache.http.concurrent.FutureCallback;

import java.util.ArrayList;

public final class TimedFutureCallback<T> implements FutureCallback<T> {

    private final FutureCallback<T> delegate;

    private final ArrayList<Timer.Context> timerContexts;

    public TimedFutureCallback(FutureCallback<T> delegate,
                               ArrayList<Timer.Context> timerContexts) {
        this.delegate = delegate;
        this.timerContexts = timerContexts;
    }

    public void completed(T result) {
        stopTimerContexts();
        delegate.completed(result);
    }

    public void failed(Exception ex) {
        stopTimerContexts();
        delegate.failed(ex);
    }

    public void cancelled() {
        stopTimerContexts();
        delegate.cancelled();
    }

    private void stopTimerContexts() {
        if (timerContexts != null) {
            for (Timer.Context timerContext : timerContexts) {
                timerContext.stop();
            }
        }
    }

}
