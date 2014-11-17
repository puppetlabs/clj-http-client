## 0.4.0
 This is a feature release which has some breaking changes.

 * Support for non-client bound asynchronous requests has been removed from both
   the Clojure and Java-layer APIs.  This includes all of the request functions
   that previously existed in the `client.async` Clojure namespace and the
   request methods in the `AsyncHttpClient` Java class.
 * Add a Java-layer API for getting an instance of an HttpClient on which
   multiple requests -- e.g.., GET, POST -- can be made.  Clients are created
   via the `createClient` method on the new `Async` and `Sync` classes, for
   a client that can make asynchronous or synchronous web requests, respectively.
 * Non-client bound synchronous requests can still be performed through the Java
   API but must now be done through the `Sync` classes rather than the
   `SyncHttpClient` class.  The `SyncHttpClient` class is now used as the type
   of the instance that the `Sync.createClient()` method returns.
 * The Java `RequestOptions` class was refactored into new `ClientOptions` and
   `RequestOptions` classes which can be used with the client-bound `Async`
   and `Sync` APIs.  For non-client bound requests, options are now defined
   via a `SimpleRequestOptions` class.
 * Reworked connection close behavior to more robustly handle successful and
   failed connections.

## 0.3.1
 This is a bugfix release.

 * Fix a memory leak that occurred as a result of connection failures.

## 0.3.0
 This is a feature release.

 * Add configuration settings for SSL Protocols and Cipher Suites to both the
   Java and Clojure clients.

## 0.2.8
 This is a bugfix release.

 * Fix a bug in the Java client API that caused a file descriptor leak each time
   a client was created for a new request.

## 0.2.7
 This is a bugfix release.

 * Fix a bug where the character encoding was always being set to ISO-8859-1 w/o
   a charset ever being explicitly specified in the Content-Type header.  We now
   honor the existing charset if there is one in the header, and otherwise we
   use UTF-8 and explicitly add the charset to the header.

## 0.2.6
 * Add :follow-redirects and :force-redirects options to the clojure client.
 * Add followRedirects and forceRedirects options to the Java client.

## 0.2.5
 * Add an overloaded constructor for `RequestOptions` that accepts a String uri

## 0.2.4
 * Fix a bug in the Java client API that caused an NPE if a Content-Type header
   did not specify a charset

## 0.2.3
 * No changes

## 0.2.2
 * Add back in support for query-params map in Clojure API

## 0.2.1
 * Upgrade to Apache HttpAsyncClient v4.0.2 (fixes a bug where headers don't get
   included when following redirects).

## 0.2.0
 * Port the code to use the Apache HttpAsyncClient library instead of
   http-kit.
 * The API around creating a persistent client has changed and
   persistent clients are explicitly managed
 * The available request options have changed. Some convenience options
   have been removed.

## 0.1.7
 * Explicitly target JDK6 when building release jars

## 0.1.6
 * Add support for configuring client SSL context with a CA cert without a
   client cert/key

## 0.1.5
 * Update to latest version of puppetlabs/kitchensink
 * Use puppetlabs/certificate-authority for SSL tasks

## 0.1.4
 * Fix bug in sync.clj when excluding clojure.core/get
 
## 0.1.3
 * Added a Java API for the synchronous client

