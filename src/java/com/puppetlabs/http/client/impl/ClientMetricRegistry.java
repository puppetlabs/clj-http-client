package com.puppetlabs.http.client.impl;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMetricRegistry extends MetricRegistry{

    private final MetricRegistry metricRegistry;
    private final ConcurrentHashMap<String, String> urlMetricNames;
    private final ConcurrentHashMap<String, String> urlMethodMetricNames;
    private final ConcurrentHashMap<ArrayList<String>, String> metricIdMetricNames;

    public ClientMetricRegistry(MetricRegistry metricRegistry) {
      this.metricRegistry = metricRegistry;
      this.urlMetricNames = new ConcurrentHashMap<String, String>();
      this.urlMethodMetricNames = new ConcurrentHashMap<String, String>();
      this.metricIdMetricNames = new ConcurrentHashMap<ArrayList<String>, String>();
    }

    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }

    public ConcurrentHashMap<String, String> getUrlMethodMetricNames() {
        return urlMethodMetricNames;
    }

    public ConcurrentHashMap<String, String> getUrlMetricNames() {
        return urlMetricNames;
    }

    public ConcurrentHashMap<ArrayList<String>, String> getMetricIdMetricNames() {
        return metricIdMetricNames;
    }

    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        return this.metricRegistry.register(name, metric);
    }

    @Override
    public void registerAll(MetricSet metrics) throws IllegalArgumentException {
        this.metricRegistry.registerAll(metrics);
    }

    @Override
    public Counter counter(String name) {
        return this.metricRegistry.counter(name);
    }

    @Override
    public Histogram histogram(String name) {
        return this.metricRegistry.histogram(name);
    }

    @Override
    public Meter meter(String name) {
        return this.metricRegistry.meter(name);
    }

    @Override
    public Timer timer(String name) {
        return this.metricRegistry.timer(name);
    }

    @Override
    public boolean remove(String name) {
        return this.metricRegistry.remove(name);
    }

    @Override
    public void removeMatching(MetricFilter filter) {
        this.metricRegistry.removeMatching(filter);
    }

    @Override
    public void addListener(MetricRegistryListener listener) {
        this.metricRegistry.addListener(listener);
    }

    @Override
    public void removeListener(MetricRegistryListener listener) {
        this.metricRegistry.removeListener(listener);
    }

    @Override
    public SortedSet<String> getNames() {
        return this.metricRegistry.getNames();
    }

    @Override
    public SortedMap<String, Gauge> getGauges() {
        return this.metricRegistry.getGauges();
    }

    @Override
    public SortedMap<String, Gauge> getGauges(MetricFilter filter) {
        return this.metricRegistry.getGauges(filter);
    }

    @Override
    public SortedMap<String, Counter> getCounters() {
        return this.metricRegistry.getCounters();
    }

    @Override
    public SortedMap<String, Counter> getCounters(MetricFilter filter) {
        return this.metricRegistry.getCounters(filter);
    }

    @Override
    public SortedMap<String, Histogram> getHistograms() {
        return this.metricRegistry.getHistograms();
    }

    @Override
    public SortedMap<String, Histogram> getHistograms(MetricFilter filter) {
        return this.metricRegistry.getHistograms(filter);
    }

    @Override
    public SortedMap<String, Meter> getMeters() {
        return this.metricRegistry.getMeters();
    }

    @Override
    public SortedMap<String, Meter> getMeters(MetricFilter filter) {
        return this.metricRegistry.getMeters(filter);
    }

    @Override
    public SortedMap<String, Timer> getTimers() {
        return this.metricRegistry.getTimers();
    }

    @Override
    public SortedMap<String, Timer> getTimers(MetricFilter filter) {
        return this.metricRegistry.getTimers(filter);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return this.metricRegistry.getMetrics();
    }
}
