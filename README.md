[![Build Status](https://travis-ci.org/puppetlabs/clj-http-client.png?branch=master)](https://travis-ci.org/puppetlabs/clj-http-client)

# puppetlabs/http-client

This is a wrapper around the [Apache HttpAsyncClient
library](http://hc.apache.org/httpcomponents-asyncclient-4.0.x/) providing
some extra functionality for configuring SSL in a way compatible with Puppet.

Async versions of the http methods are exposed in
puppetlabs.http.client.async, and synchronous versions are in
puppetlabs.http.client.sync. For information on using these namespaces, see the page on
[making requests with clojure clients](doc/clojure-client.md).

Additionally, this library allows you to make requests using Java clients. For information
on how to do this, see the page on [making requests with java clients](doc/java-client.md).
