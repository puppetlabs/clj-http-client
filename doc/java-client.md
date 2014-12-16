## Making requests with the Java client

Similarly to the way it is done in clojure code, clj-http-client allows you to make requests
in two ways using Java: with and without a persistent client.

## `createClient(ClientOptions clientOptions)`

clj-http-client allows you to create a persistent synchronous or asynchronous HTTP client using the static
`createClient()` method in the `Async` and `Sync` classes

This method takes one argument, `clientOptions`, which is an instance of the `ClientOptions` class. `ClientOptions`
has two constructors:

```java
public ClientOptions();
public ClientOptions(SSLContext sslContext,
              String sslCert,
              String sslKey,
              String sslCaCert,
              String[] sslProtocols,
              String[] sslCipherSuites,
              boolean insecure,
              boolean forceRedirects,
              boolean followRedirects);
```

Each parameter in the second constructor is an option corresponding to options in the clojure `create-client` function.
See the page on [making requests with clojure clients](clojure-client.md) for more information.


### Making requests with a persistent client

The `createClient()` method returns an object with a number of request methods. For example, the
`createClient()` method in Sync.java would return an object implementing

```java
public Response request(RequestOptions requestOptions, HttpMethod method);

public Response get(String url) throws URISyntaxException;
public Response get(URI uri);
public Response get(RequestOptions requestOptions);

public Response head(String url) throws URISyntaxException;
public Response head(URI uri);
public Response head(RequestOptions requestOptions);

public Response post(String url) throws URISyntaxException;
public Response post(URI uri);
public Response post(RequestOptions requestOptions);

public Response put(String url) throws URISyntaxException;
public Response put(URI uri);
public Response put(RequestOptions requestOptions);

public Response delete(String url) throws URISyntaxException;
public Response delete(URI uri);
public Response delete(RequestOptions requestOptions);

public Response trace(String url) throws URISyntaxException;
public Response trace(URI uri);
public Response trace(RequestOptions requestOptions);

public Response options(String url) throws URISyntaxException;
public Response options(URI uri);
public Response options(RequestOptions requestOptions);

public Response patch(String url) throws URISyntaxException;
public Response patch(URI uri);
public Response patch(RequestOptions requestOptions);

public void close();
```

Each method will execute the corresponding HTTP request, with the exception of `close`, which
will close the client.

Each request method has three signatures. The first takes one argument, `String url`, which is the URL
against which you want to make a request. The second takes one argument, `URI uri`, which is the URI against
which you want to make a request.

The third takes a `RequestOptions` object. `RequestOptions` is an object allowing you to set options for a request.
This object has three constructors:

```java
public RequestOptions (String url);
public RequestOptions(URI uri);
public RequestOptions (URI uri,
                       Map<String, String> headers,
                       Object body,
                       boolean decompressBody,
                       ResponseBodyType as);
```

The first constructor takes one argument, `String url`, which is the URL against which you want to make the
request. The second takes one argument, `URI uri`, which is the URI against which you want to make the request.
The third takes five arguments. The first, `URI uri`, is the same as the `uri` argument in the second constructor.
The rest correspond to the similarly named options that can be passed to the clojure request functions. For more
details, see the page on [making requests with the clojure client](clojure-client.md). The `RequestOptions` class
provides getters and setters for all options.

Note that the `RequestOptions` object has no `query-params` option like in the clojure request functions. All query
parameters should be set in the `uri`.

A synchronous HTTP client also provides the `request` method. This method takes two arguments:
`RequestOptions requestOptions`, and `HttpMethod method`. `HttpMethod` is an enum with the following fields:
`GET, HEAD, POST, PUT, DELETE, TRACE, OPTIONS, PATCH`.

The object returned by the `createClient()` method in the `Async` class is nearly identical to that returned by the
`createClient()` method in the `Sync` class. However, the various request methods return a `Promise<Response>` instead
of a `Response`, and the async client does NOT provide a `request` method.

## Making a Request without a persistent client

In addition to allowing you to create a persistent client with the `createClient()` method, the
`Sync` class provides the following simple request methods that can be
called without a client:

```java
public Response get(String url) throws URISyntaxException;
public Response get(URI uri);
public Response get(SimpleRequestOptions simpleRequestOptions);

public Response head(String url) throws URISyntaxException;
public Response head(URI uri);
public Response head(SimpleRequestOptions simpleRequestOptions);

public Response post(String url) throws URISyntaxException;
public Response post(URI uri);
public Response post(SimpleRequestOptions simpleRequestOptions);

public Response put(String url) throws URISyntaxException;
public Response put(URI uri);
public Response put(SimpleRequestOptions simpleRequestOptions);

public Response delete(String url) throws URISyntaxException;
public Response delete(URI uri);
public Response delete(SimpleRequestOptions simpleRequestOptions);

public Response trace(String url) throws URISyntaxException;
public Response trace(URI uri);
public Response trace(SimpleRequestOptions simpleRequestOptions);

public Response options(String url) throws URISyntaxException;
public Response options(URI uri);
public Response options(SimpleRequestOptions simpleRequestOptions);

public Response patch(String url) throws URISyntaxException;
public Response patch(URI uri);
public Response patch(SimpleRequestOptions simpleRequestOptions);
```
These methods will, for every request, create a new client, make a new request with that client, and then
close the client once the response is received. These are similar to the request methods provided by a
persistent HTTP client, with one notable exception: they take a `SimpleRequestOptions` object rather than a
`RequestOptions` object.

`SimpleRequestOptions` provides two constructors:

```java
public SimpleRequestOptions (String url);
public SimpleRequestOptions(URI uri);
```

The `url` and `uri` arguments are the URL and the URI against which you want to make your request.
`SimpleRequestOptions` also has a number of options which can be set using corresponding setter methods.
These options are listed below.

```java
URI uri;
Map<String, String> headers;
SSLContext sslContext;
String sslCert;
String sslKey;
String sslCaCert;
String[] sslProtocols;
String[] sslCipherSuites;
boolean insecure;
Object body;
boolean decompressBody;
ResponseBodyType as;
boolean forceRedirects;
boolean followRedirects;
```

The options are simply the union of the options available in the `ClientOptions` and `RequestOptions` classes.