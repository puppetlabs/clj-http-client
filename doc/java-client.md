## Making requests with the Java client

Similarly to the way it is done in clojure code, clj-http-client allows you to make requests
in two ways using Java: with and without a persistent client.

## `createClient(ClientOptions clientOptions)`

clj-http-client allows you to create a persistent synchronous or asynchronous HTTP client using the static
`createClient()` method in the [`Async`](../src/java/com/puppetlabs/http/client/Async.java) and
[`Sync`](../src/java/com/puppetlabs/http/client/Sync.java) classes

This method takes one argument, `clientOptions`, which is an instance of the
[`ClientOptions`](../src/java/com/puppetlabs/http/client/ClientOptions.java) class, details on which can
be found in its javadoc strings, linked above.

### Making requests with a persistent client

The `createClient()` method returns an object implementing the [`SyncHttpClient`](../src/java/com/puppetlabs/http/client/SyncHttpClient.java)
interface in the case of `Sync`, and the [`AsyncHttpClient`](../src/java/com/puppetlabs/http/client/AsyncHttpClient.java) interface
in the case of `Async`. Information on the various methods available is detailed in the javadoc strings for the corresponding
interfaces, which are linked above. The various request methods provided by these interfaces can take
a [`RequestOptions`](../src/java/com/puppetlabs/http/client/RequestOptions.java) object, information on
which can be found in that class' javadoc strings, linked above.

For example, say you have a Persistent synchronous client, `client`, and you want to make a GET request
against the URL `http://localhost:8080/test` with query parameter `abc` with value `def`. To make the request
and print the body of the response, you could do the following:

```java
Response response = client.get(new URI("http://localhost:8080/test?abc=def"));
System.out.println(response.getBody());
```

If `client` was instead asynchronous, you would do the following:

```java
Promise<Response> response = client.get(new URI("http://localhost:8080/test?abc=def"));
System.out.println(response.deref().getBody());
```

### Closing the client

Each persistent client provides a `close` method, which can be used to close the client. This method will close
the client and clean up all resources associated with it. It must be called by the caller when finished using the
client to make requests, as there is no implicit cleanup of the associated resources when the client is garbage
collected. Once the client is closed, it can no longer be used to make requests.

## Making a Request without a persistent client

In addition to allowing you to create a persistent client with the `createClient()` method, the
[`Sync`](../src/java/com/puppetlabs/http/client/Sync.java) class contains a number of simple request methods
that allow for requests to be made without a persistent client. These are detailed in `Sync.java`'s
javadoc strings, linked above. Many of the provided request methods take a
[`SimpleRequestOptions`](../src/java/com/puppetlabs/http/client/SimpleRequestOptions.java) object. Information
on this class can be found in its javadoc strings, linked above.

As an example, say you wanted to make a request to the URL `http://localhost:8080/test` without a persistent client.
You want the query parameter `abc` with value `def`, and you don't want redirects to be followed. In that case, you
would do the following to print the body of the response:

```java
SimpleRequestOptions options = new SimpleRequestOptions(new URI("http://localhost:8080/test?abc=def"));
options = options.setFollowRedirects(false);
Response response = Sync.get(options);
System.out.println(response.getBody());
```