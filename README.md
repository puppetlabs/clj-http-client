# puppetlabs/http-client

[![Build Status](https://travis-ci.com/puppetlabs/clj-http-client.png?branch=master)](https://travis-ci.com/puppetlabs/clj-http-client)

This is a wrapper around the [Apache HttpAsyncClient
library](https://hc.apache.org/httpcomponents-asyncclient-4.1.x/index.html) providing
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

## Testing

The tests require pki files in the `dev-resources/ssl/` directory of:
  * `ca.pem`: a CA cert with the CN of "puppet"
  * `key.pem`: a node private key
  * `cert.pem`: a cert signed by `ca.pem` for the private key at `key.pem` with a CN of "localhost"
  * `alternate-ca.pem`: a valid but untrusted CA cert

The repo contains these files needed for testing, though if needed you may
want to read `dev-resources/gen-pki.sh` for the commands to generate additional
sets of files.

## Upgrading dependencies

The APIs provided by httpclient change significantly in 5.0 and some of the
internal classes that we've extended change within minor releases of the 4.5
series. Whenever upgrading the apache/httpcomponents dependencies an audit of
the Java classes should be undertaken, especially the classes pertaining to
SafeRedirectedRequest and RedirectStrategy.

## Support

We use the [Trapperkeeper project on JIRA](https://tickets.puppetlabs.com/browse/TK)
for tickets on clj-http-client, although Github issues are welcome too.
