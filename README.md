# puppetlabs/http-client

[![Build Status](https://travis-ci.org/puppetlabs/clj-http-client.png?branch=master)](https://travis-ci.org/puppetlabs/clj-http-client)

This is a wrapper around the [Apache HttpAsyncClient
library](http://hc.apache.org/httpcomponents-asyncclient-4.0.x/) providing
some extra functionality for configuring SSL in a way compatible with Puppet.

## Installation

Add the following dependency to your `project.clj` file:

[![Clojars Project](http://clojars.org/puppetlabs/http-client/latest-version.svg)](http://clojars.org/puppetlabs/http-client)

## Details

Async versions of the http methods are exposed in
puppetlabs.http.client.async, and synchronous versions are in
puppetlabs.http.client.sync. For information on using these namespaces, see the page on
[making requests with clojure clients](doc/clojure-client.md).

Additionally, this library allows you to make requests using Java clients. For information
on how to do this, see the page on [making requests with java clients](doc/java-client.md).

## Support

We use the [Trapperkeeper project on JIRA](https://tickets.puppetlabs.com/browse/TK)
for tickets on clj-http-client, although Github issues are welcome too.
