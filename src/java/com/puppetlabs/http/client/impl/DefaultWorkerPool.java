package com.puppetlabs.http.client.impl;

import org.httpkit.PrefixThreadFactory;

import java.util.concurrent.*;

public class DefaultWorkerPool {

    private static ExecutorService instance;

    public static synchronized ExecutorService getInstance() {
        if (instance == null) {
            int max = Runtime.getRuntime().availableProcessors();
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
            PrefixThreadFactory factory = new PrefixThreadFactory("client-worker-");

            instance = new ThreadPoolExecutor(0, max, 60, TimeUnit.SECONDS, queue, factory);
        }
        return instance;
    }
}
