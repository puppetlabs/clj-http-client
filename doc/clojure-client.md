## Making requests with clojure clients

clj-http-client allows you to make requests in two ways with clojure clients: with and without a persistent HTTP client.

## `create-client`

clj-http-client allows you to create a persistent synchronous or asynchronous HTTP client using the
`create-client` function from the corresponding namespace.

The `create-client` function takes one argument, a map called `options`. The available options
for configuring the client are detailed below.

### Base Options

The following are the base set of options supported by the `create-client` functions.

* `:force-redirects`: used to set whether or not the client should follow
  redirects on POST or PUT requests. Defaults to false.
* `:follow-redirects`: used to set whether or not the client should follow
  redirects in general. Defaults to true. If set to false, will override
  the :force-redirects setting.
* `:ssl-protocols`: an array used to set the list of SSL protocols that the client
  could select from when talking to the server. Defaults to 'TLSv1',
  'TLSv1.1', and 'TLSv1.2'.
* `:cipher-suites`: an array used to set the cipher suites that the client could
  select from when talking to the server. Defaults to the complete
  set of suites supported by the underlying language runtime.

### SSL Options

The following options are SSL specific, and only one of the following combinations is permitted.

* `:ssl-context`: an instance of [SSLContext](http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLContext.html)

OR

* `:ssl-cert`: path to a PEM file containing the client cert
* `:ssl-key`: path to a PEM file containing the client private key
* `:ssl-ca-cert`: path to a PEM file containing the CA cert

OR

* `:ssl-ca-cert`: path to a PEM file containing the CA cert

### Making requests with a persistent client

The `create-client` functions return a client
with the following protocol:

```clj
(defprotocol HTTPClient
  (get [this url] [this url opts])
  (head [this url] [this url opts])
  (post [this url] [this url opts])
  (put [this url] [this url opts])
  (delete [this url] [this url opts])
  (trace [this url] [this url opts])
  (options [this url] [this url opts])
  (patch [this url] [this url opts])
  (close [this]))

```

Each function will execute the corresponding HTTP request, with the exception of `close`, which
will close the client.

Each request function takes one argument, `url`, which is the URL against which you want to make
your HTTP request. Each request function also has a two-arity version with an extra parameter, `options`,
which is a map containing options for the HTTP request. These options are as follows:

* `:headers`: optional; a map of headers
* `:body`: optional; may be a String or any type supported by clojure's reader
* `:decompress-body`: optional; if `true`, an 'accept-encoding' header with a value of
  'gzip, deflate' will be added to the request, and the response will be
   automatically decompressed if it contains a recognized 'content-encoding'
   header.  Defaults to `true`.
* `:as`: optional; used to control the data type of the response body.  Supported values
  are `:text` and `:stream`, which will return a `String` or an
  `InputStream`, respectively.  Defaults to `:stream`.
* `:query-params`: optional; used to set the query parameters of an http request. This should be
  a map, where each key and each value is a String.

For example, say you want to make a GET request with
query parameter `abc` with value `def` to the URL `http://localhost:8080/test`. If you wanted to use a
persistent synchronous client, you could make the request and print the body of the response like so:

```clj
(let [client   (sync/create-client {})
      response (get client "http://localhost:8080/test" {:query-params {"abc" "def"}})]
  (println (:body response))
```

If you wanted to use an asynchronous client, you could make the request and print the body of the response like so:

```clj
(let [client   (async/create-client {})
      response (get client "http://localhost:8080/test" {:query-params {"abc" "def"}})]
  (println (:body @response)))
```

### Closing a persistent client

The `close` function takes no arguments. This function closes the client, and causes
all resources associated with it to be cleaned up. This function must be called by the caller when
they are done making requests with the client, as no implicit cleanup of the associated resources
is done when the client is garbage collected. Once a client is closed, it can no longer be used to
make any requests.

## Making a Request without a persistent client

In addition to allowing you to create a persistent client with the `create-client` function, the
puppetlabs.http.client.sync namespace provides the following simple request functions that can be
called without a client:

```clj
(get [url] [url opts])
(head [url] [url opts])
(post [url] [url opts])
(put [url] [url opts])
(delete [url] [url opts])
(trace [url] [url opts])
(options [url] [url opts])
(patch [url] [url opts])
(request [req])

```
These functions will, for every request, create a new client, make a new request with that client, and then
close the client once the response is received. Each of these functions (barring `request`) take one argument,
`url`, which is the URL to which you want to make the request, and can optionally take a second argument, `options`.
`options` is a map of options to configure both the client and the request, and as such takes the union of all options
accepted by the `create-client` function and all options accepted by the request functions for a persistent
client.

For example, say you want to make a GET request to the URL `http://localhost:8080/test` with query parameter
`abc` with value `def`, and you do not want redirects to be followed. In that case, you could do the following
to make the request and print the body of the response:

```clj
(let [response (get "http://localhost:8080/test" {:follow-redirects false
                                                  :query-params {"abc" "def"}})]
  (println (:body response)))
```

A `request` function is also provided, which allows you to make a request of any type.
`request` takes one argument, `req`, which is a map of options. It takes the same options as the simple request
functions, but also takes the following required options:

* `:url`: the URL against which to make the request. This should be a string.
* `:method`: the HTTP method (:get, :head, :post, :put, :delete, :trace, :options, :patch)