package com.puppetlabs.http.client.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncClose {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AsyncClose.class);

    private static final ExecutorService closerPool =
            Executors.newFixedThreadPool(3);

    private static class RunnableCloser implements Runnable {

        private Closeable closeable;

        public RunnableCloser(Closeable closeable) {
            this.closeable = closeable;
        }

        @Override
        public void run() {
            try {
                this.closeable.close();
            } catch (IOException ioe) {
                LOGGER.error("Async close error", ioe);
                throw new RuntimeException("Async close error", ioe);
            }
        }
    }

    public static void close (Closeable closeable) {
        if (closeable != null) {
            closerPool.submit(new RunnableCloser(closeable));
        }
    }

}
