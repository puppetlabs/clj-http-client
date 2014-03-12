# puppetlabs/http-client

This is a wrapper around the [http-kit](http://http-kit.org/) client
providing some extra functionality for configuring SSL in a way
compatible with Puppet.

Async versions of the http methods are exposed in
puppetlabs.http.client, and synchronous versions are in
puppetlabs.http.sync.

If you are used to the http-kit API note that in this version all
methods take the options map last, whereas in http-kit the callback is
last.
