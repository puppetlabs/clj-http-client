## 0.2.2
 * Add back in support for query-params map in Clojure API

## 0.2.1
 * Upgrade to Apache HttpAsyncClient v4.0.2 (fixes a bug where headers don't get included
   when following redirects).

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
 * Add support for configuring client SSL context with a CA cert without a client cert/key

## 0.1.5
 * Update to latest version of puppetlabs/kitchensink
 * Use puppetlabs/certificate-authority for SSL tasks

## 0.1.4
 * Fix bug in sync.clj when excluding clojure.core/get
 
## 0.1.3
 * Added a Java API for the synchronous client

