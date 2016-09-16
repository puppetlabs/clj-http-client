# Metrics

Both the Java client and the Clojure client have [Dropwizard
Metrics](http://metrics.dropwizard.io/3.1.0/) support - they both accept as an
option a `MetricRegistry` to which they will register http metrics for each
request, as well as metrics for an metric-ids specified in the request
options. This support is experimental - names of metrics and the exact API may
change.

For using metrics with either the Java client or the Clojure client you must
already have created a Dropwizard `MetricRegistry`.

- [Metrics prefix](#metrics-prefix)
- [Types of metrics](#types-of-metrics)
- [Getting back metrics](#getting-back-metrics)
- [Clojure API](#clojure-api)
  - [Creating a client with metrics](#creating-a-client-with-metrics)
  - [Setting a metric-id](#setting-a-metric-id)
  - [Filtering metrics](#filtering-metrics)
    - [Filtering by URL](#filtering-by-url)
    - [Filtering by URL and method](#filtering-by-url-and-method)
    - [Filtering by metric-id](#filtering-by-metric-id)
- [Java API](#java-api)
  - [Creating a client with metrics](#creating-a-client-with-metrics-1)
  - [Setting a metric-id](#setting-a-metric-id-1)
  - [Filtering metrics](#filtering-metrics-1)
    - [Filtering by URL](#filtering-by-url-1)
    - [Filtering by URL and method](#filtering-by-url-and-method-1)
    - [Filtering by metric-id](#filtering-by-metric-id-1)


## Metrics prefix

All http metrics are prefixed with `puppetlabs.http-client.experimental`.

## Types of metrics

There are two types of metrics: *full response* and *initial response*. Full
response metrics stop when all bytes of the response have been read by the
client. Initial response metrics stop when the first byte of the response has
been received by the client. Full response metrics are suffixed with
`full-response`. Initial response metric support has not yet been
implementedare suffixed with `full-response`. Initial response metric support
has not yet been implemented.

There are three categories of metrics: `url` metrics, `url-and-method`
metrics, and `metric-id` metrics. `url` and `url-and-method` metrics are
created automatically for each request; `metric-id` metrics are only created
for requests that have a `metric-id` request option specified.

Each http request will have a metric name created for its url (stripped of
query strings and url fragments), as well as for the url + method name. `url`
metrics have `with-url` after the prefix, followed by the url.
`url-and-method` metrics have `with-url-and-method` after the prefix, followed
by the url and then the capitalized HTTP method.

So, for example,  a `GET` request to `http://foobar.com` would create a metric
`puppetlabs.http-client.experimental.with-url.http://foobar.com.full-response`
and a metric
`puppetlabs.http-client.experimental.with-url-and-method.http://foobar.com.GET.full-response`.

It is also possible to give a request a `metric-id`. The `metric-id` is an
array of strings.  For each element in the array, a metric name will be
created, appending to the previous elements. `metric-id` metrics have
`with-metric-id` after the metric prefix, followed by the metric-id.

So, for example, for a metric-id `["foo", "bar", "baz"]`, the metrics
`puppetlabs.http-client.experimental.with-metric-id.foo.full-response`,
`puppetlabs.http-client.experimental.with-metric-id.foo.bar.full-response`,
and
`puppetlabs.http-client.experimental.with-metric-id.foo.bar.baz.full-response`
would be created.

## Getting back metrics

Both the Clojure API and the Java API have functions to get back from a
`MetricRegistry` either the `Timer` objects or a selection of metric data, and
to filter this information based on url, url and method, or metric-id.

There are three different `Timer` objects: `UrlClientTimer` (which includes a
field for the url and `getUrl` method), `UrlAndMethodClientTimer` (which
includes a field for the url and a field for the method and accompanying
`getUrl` and `getMethod` methods), and `MetricIdClientTimer` (which includes a
field for the metric-id and accompanying `getMetricId` method). Each of these
timers also has an `isCategory` method that returns whether the timer is or is
not the provided `MetricCategory`.

Both the Clojure and Java APIs also include functions that return a list of
metrics data.  This metrics data includes the `metric-name`, `count`, `mean`
(in ms), and `aggregate` (computed as `count * mean`) for each metric. In
addition, for `url` metrics the accompanying metrics data includes the `url`,
for `url-and-method` metrics it includes the `url` and `method`, and for
`metric-id` metrics it includes the `metric-id`.

The Java API returns `ClientMetricData` objects - of which there are three
types - `UrlClientMetricData`, `UrlAndMethodClientMetricData`, and
`MetricIdClientMetricData`. The Clojure API returns maps with keys for this
information.

Both APIs have functions for returning all metrics/metric data, for returning
all metrics/metrics data from a specific category, and for filtering
metrics/metrics data within a category.

## Clojure API

### Creating a client with metrics
To use metrics with the Clojure client, pass a `MetricRegistry` to it as part
of the options map:

```clojure
(async/create-client {:metric-registry (MetricRegistry.)})
```

(the same works with the `sync` client).

In [Trapperkeeper](https://github.com/puppetlabs/trapperkeeper) applications,
creating and managing a `MetricRegistry` can be done easily with
[trapperkeeper-metrics](https://github.com/puppetlabs/trapperkeeper-metrics):

```clojure
(defservice my-trapperkeeper-service
  MyService
  [[:MetricsService get-metrics-registry]]
  (init [this context]
    (let [registry (get-metrics-registry)
          client (async/create-client {:metric-registry registry})]
      ...)))
```

Any client that is created with a `MetricRegistry` will automatically have
`url` and `url-and-method` metrics registered.

`get-client-metric-registry` protocol function can be called on a client to
get the `MetricRegistry` from it.

### Setting a metric-id

In addition to the `url` and `url-and-method` metrics, it is possible to set a
`metric-id` for a request that will create additional metrics.

For the Clojure API, a `metric-id` is a vector of keywords or strings. Either
is supported by the API, however, if special characters are needed, use
strings.  Note than even when specified as a vector of keywords in the request
option, the metric-id will be returned as a vector of strings in the metrics
data.

To set a `metric-id` for a request, include it as an option in the request
options map.

```clojure
(common/get client "https://foobar.com" {:metric-id [:foo :bar :baz]})
(common/get client "https://foobar.com" {:metric-id ["f/o/o"]})
```

### Filtering metrics

To get all `Timer` objects registered for a `MetricRegistry`, use the
`get-client-metrics` function in the `metrics` namespace.  This takes the
`MetricRegistry` as an argument and returns a map with three keys: `:url`
`:url-and-method`, and `:metric-id`. Under each of these keys is a sequence of
`Timer` objects of the corresponding type. If there are no timers of a certain
type, the sequence will be empty. The output of this function conforms to the
`common/AllTimers` schema.

Example:

```clojure
(common/get client "http://test.com" {:metric-id [:foo :bar :baz]})
...
(metrics/get-client-metrics metric-registry)
=>
{:url [#object[com.puppetlabs.http.client.metrics.UrlClientTimer
               0x66cf1f05
               "com.puppetlabs.http.client.metrics.UrlClientTimer@66cf1f05"]]
 :url-and-method [#object[com.puppetlabs.http.client.metrics.UrlAndMethodClientTimer
                          0x6fe5444c
                          "com.puppetlabs.http.client.metrics.UrlAndMethodClientTimer@6fe5444c"]]
 :metric-id [#object[com.puppetlabs.http.client.metrics.MetricIdClientTimer
                     0x690c10c5
                     "com.puppetlabs.http.client.metrics.MetricIdClientTimer@690c10c5"]
             #object[com.puppetlabs.http.client.metrics.MetricIdClientTimer
                     0xb7aca2e
                     "com.puppetlabs.http.client.metrics.MetricIdClientTimer@b7aca2e"]
             #object[com.puppetlabs.http.client.metrics.MetricIdClientTimer
                     0x4ef82829
                     "com.puppetlabs.http.client.metrics.MetricIdClientTimer@4ef82829"]]}
```

To get metric data for all `Timer`s registered on a `MetricRegistry`, use the
`get-client-metrics-data` function. This takes the `MetricRegistry` and returns
a map with `:url`, `:url-and-method`, and `:metric-id` as keys. Under each of
these keys is a sequence of maps, each map containing metrics data (see
[Getting back metrics](#Getting-back-metrics) above, conforming to the
`common/AllMetricsData` schema.

Example:

```clojure
(common/get client "http://test.com" {:metric-id [:foo :bar :baz]})
...
(metrics/get-client-metrics-data metric-registry)
=>
{:url ({:count 1
        :mean 553
        :aggregate 553
        :metric-name "puppetlabs.http-client.experimental.with-url.http://test.com.full-response"
        :url "http://test.com"})
 :url-and-method ({:count 1
                   :mean 554
                   :aggregate 554
                   :metric-name "puppetlabs.http-client.experimental.with-url-and-method.http://test.com.GET.full-response"
                   :url "http://test.com"
                   :method "GET"})
 :metric-id ({:count 1
              :mean 554
              :aggregate 554
              :metric-name "puppetlabs.http-client.experimental.with-metric-id.foo.bar.baz.full-response"
              :metric-id ["foo" "bar" "baz"]}
             {:count 1
              :mean 554
              :aggregate 554
              :metric-name "puppetlabs.http-client.experimental.with-metric-id.foo.bar.full-response"
              :metric-id ["foo" "bar"]}
             {:count 1
              :mean 554
              :aggregate 554
              :metric-name "puppetlabs.http-client.experimental.with-metric-id.foo.full-response"
              :metric-id ["foo"]})}
```

#### Filtering by URL

To get URL metrics and metrics data, use the `get-client-metrics-by-url`
function and `get-client-metrics-data-by-url` in the `metrics` namespace.

Both of these take as arguments the `MetricRegistry` and a string url. If no
url is provided, return all url metrics/metrics data in a sequence. If no
metrics are registered for that url, return an empty sequence.

Example:

```clojure
(common/get client "http://test.com" {:metric-id [:foo :bar :baz]})
...
(metrics/get-client-metrics-data-by-url metric-registry "http://test.com")
=>
({:count 1
  :mean 553
  :aggregate 553
  :metric-name "puppetlabs.http-client.experimental.with-url.http://test.com.full-response"
  :url "http://test.com"})

(metrics/get-client-metrics-data-by-url metric-registry "http://not-a-matching-url.com")
=> ()
```

#### Filtering by URL and method

To get URL and method metrics and metrics data, use the
`get-client-metrics-by-url-and-method` and
`get-client-metrics-data-by-url-and-method` functions in the `metrics`
namespace.

Both of these take as arguments the `MetricRegistry`, a string url, and a
keyword HTTP method. If no url and method is provided, return all url
metrics/metrics data in a sequence. If no metrics are registered for that url
and method, return an empty sequence.

Example:

```clojure
(common/get client "http://test.com" {:metric-id [:foo :bar :baz]})
...
(metrics/get-client-metrics-data-by-url-and-method metric-registry "http://test.com" :get)
=>
({:count 1
  :mean 554
  :aggregate 554
  :metric-name "puppetlabs.http-client.experimental.with-url-and-method.http://test.com.GET.full-response"
  :url "http://test.com"
  :method "GET"})

  :method "GET"})

(metrics/get-client-metrics-data-by-url-and-method metric-registry "http://test.com" :post)
=> ()
```

#### Filtering by metric-id

To get metric-id metrics and metrics data, use the
`get-client-metrics-by-metric-id` and `get-client-metrics-data-by-metric-id`
functions in the `metrics` namespace.

Both of these take as arguments the `MetricRegistry` and a metric-id - as a
vector of keywords or strings (if special characters are needed, use strings).
If no metric-id is provided, will return all metric-id metrics.  If no metrics
are registered for that metric-id, returns an empty list.

Example:

```clojure
(common/get client "http://test.com" {:metric-id [:foo :bar :baz]})
...
(metrics/get-client-metrics-data-by-metric-id metric-registry [:foo])
=>
({:count 1
  :mean 554
  :aggregate 554
  :metric-name "puppetlabs.http-client.experimental.with-metric-id.foo.full-response"
  :metric-id ["foo"]})

(metrics/get-client-metrics-data-by-metric-id metric-registry [:foo :bar])
=>
({:count 1
  :mean 554
  :aggregate 554
  :metric-name "puppetlabs.http-client.experimental.with-metric-id.foo.bar.full-response"
  :metric-id ["foo" "bar"]})

(metrics/get-client-metrics-data-by-metric-id metric-registry [:foo :nope])
=> ()
```

## Java API

### Creating a client with metrics
To use metrics with the Java client, call `setMetricRegistry()` on the
`ClientOptions` before supplying them to create the client.

```java
MetricRegistry registry = new MetricRegistry();
ClientOptions options = new ClientOptions();
AsyncHttpClient client = Async.createClient(options);
```

(the same works with the `SyncHttpClient`).

Any client that is created with a `MetricRegistry` will automatically have
`url` and `url-and-method` metrics registered.

`getMetricRegistry()` can be called on a client to get the `MetricRegistry`
from it.

### Setting a metric-id

In addition to the `url` and `url-and-method` metrics, it is possible to set a
`metric-id` for a request that will create additional metrics.

A `metric-id` is an array of strings.

To set a `metric-id` for a request, use the `.setMetricId` method on the
`RequestOptions` class.

```java
RequestOptions options = new RequestOptions("https://foobar.com");
options.setMetricId(["foo", "bar", "baz"]);
client.get(options);
```

`getMetricId()` can be called on `RequestOptions` to get back the `metric-id`.

### Filtering metrics

To get all `Timer` objects registered for a `MetricRegistry`, use the
`getClientMetrics()` method in the `Metrics` class. This takes the
`MetricRegistry` as an argument and returns a `ClientTimerContainer` object.

The `ClientTimerContainer` object has three fields - `urlTimers`,
`urlAndMethodTimers`, and `metricIdTimers`. The list of `URLClientTimers` can
be retrieved from the `ClientTimerContainer` with the `getUrlTimers()` method.
A list of `URlAndMethodClientTimers` can be retrieved with the
`getUrlAndMethod()` method. A list of `MetricIdClientTimer`s can be retrieved
with the `getMetricIdTimers()` method.

To get all `MetricData` objects, representing data for each `Metric`
registered for a `MetricRegistry`, use the `getClientMetricsData()` methods in
the `Metric` class. This takes the `MetricRegistry` as an argument and returns
a `ClientMetricDataContainer` object. This object has a field for each type of
metric data - `urlData`, `urlAndMethodData`, and `metricIdData`. Each field
has an associated getter returning a list of the appropriate data object -
`UrlClientMetricData`, `UrlAndMethodClientMetricData`, and
`MetricIdClientMetricData`.

#### Filtering by URL

To get URL metrics and metrics data, use the `getClientMetricsByUrl()` and
`getClientMetricsDataByUrl()` methods in the `Metric` class.

Both of these take as arguments the `MetricRegistry` and a string url. If no
url is provided, return all url metrics. If no metrics are registered for that
url, return an empty list.

`getClientMetricsByUrl()` returns a list of `UrlClientTimer` objects.
`getClientMetricsDataByUrl` returns a list of `UrlClientMetricData` objects.

#### Filtering by URL and method

To get URL and method metrics and metrics data, use the
`getClientMetricsByUrlAndMethod()` and `getClientMetricsDataByUrlAndMethod()`
methods in the `Metric` class.

Both of these take as arguments the `MetricRegistry`, a string url, and a
string HTTP method. If no url or method is provided, will return all
url-and-method metrics. If no metrics are registered for that url and method,
returns an empty list.

`getClientMetricsByUrlAndMethod()` returns a list of `UrlAndMethodClientTimer`
objects. `getClientMetricsDataByUrlAndMethod` returns a list of
`UrlAndMethodClientMetricData` objects.

#### Filtering by metric-id

To get metric-id metrics and metrics data, use the
`getClientMetricsByMetricId()` and `getClientMetricsDataByMetricId()` methods
in the `Metric` class.

Both of these take as arguments the `MetricRegistry` and a metric-id - as an
array of strings. If no metric-id is provided, will return all metric-id
metrics. If no metrics are registered for that metric-id, returns an empty
list.

`getClientMetricsByMetricId()` returns a list of `MetricIdClientTimer`
objects. `getClientMetricsDataByMetricId()` returns a list of
`MetricIdClientMetricData` objects.
