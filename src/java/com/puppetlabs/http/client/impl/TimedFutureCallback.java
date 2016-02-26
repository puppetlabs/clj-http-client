package com.puppetlabs.http.client.impl;

import com.codahale.metrics.Timer;
import org.apache.http.concurrent.FutureCallback;

public interface TimedFutureCallback<T> extends FutureCallback<T>{

        void timedCompleted(T result, Timer.Context timer);

        void timedFailed(Exception ex, Timer.Context timer);

        void timedCancelled(Timer.Context timer);
}
