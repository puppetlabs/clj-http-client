;; This namespace is a wrapper around the http client functionality provided
;; by Apache HttpAsyncClient. It allows the use of PEM files for HTTPS configuration.
;;
;; In the options to any request method, an existing SSLContext object can be
;; supplied under :ssl-context. If this is present it will be used. If it's
;; not, the wrapper will attempt to use a set of PEM files stored in
;; (:ssl-cert :ssl-key :ssl-ca-cert) to create the SSLContext.
;;
;; See the puppetlabs.http.sync namespace for synchronous versions of all
;; these methods.

(ns puppetlabs.http.client.async
  (:import (com.puppetlabs.http.client ClientOptions RequestOptions ResponseBodyType HttpMethod CompressType)
           (com.puppetlabs.http.client.impl JavaClient ResponseDeliveryDelegate)
           (org.apache.http.impl.nio.client CloseableHttpAsyncClient)
           (org.apache.http.client.utils URIBuilder)
           (org.apache.http.entity ContentType)
           (org.apache.http.nio.client HttpAsyncClient)
           (com.codahale.metrics MetricRegistry)
           (java.util Locale)
           (java.net URI URL))

  (:require [puppetlabs.http.client.common :as common]
            [schema.core :as schema]
            [puppetlabs.http.client.metrics :as metrics]
            [puppetlabs.i18n.core :as i18n :refer [trs]]
            [clojure.string :as str])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private utility functions

(schema/defn ^:always-validate create-default-client :- CloseableHttpAsyncClient
  [{:keys [ssl-context ssl-ca-cert ssl-cert ssl-key ssl-protocols cipher-suites
           follow-redirects force-redirects connect-timeout-milliseconds
           socket-timeout-milliseconds metric-registry server-id
           metric-prefix enable-url-metrics?
           max-connections-total
           max-connections-per-route]}:- common/ClientOptions]
  (let [client-options (ClientOptions.)]
    (cond-> client-options
            (some? ssl-context) (.setSslContext ssl-context)
            (some? ssl-cert) (.setSslCert ssl-cert)
            (some? ssl-ca-cert) (.setSslCaCert ssl-ca-cert)
            (some? ssl-key) (.setSslKey ssl-key)
            (some? ssl-protocols) (.setSslProtocols (into-array String ssl-protocols))
            (some? cipher-suites) (.setSslCipherSuites (into-array String cipher-suites))
            (some? force-redirects) (.setForceRedirects force-redirects)
            (some? follow-redirects) (.setFollowRedirects follow-redirects)
            (some? connect-timeout-milliseconds)
            (.setConnectTimeoutMilliseconds connect-timeout-milliseconds)
            (some? socket-timeout-milliseconds)
            (.setSocketTimeoutMilliseconds socket-timeout-milliseconds)
            (some? metric-registry) (.setMetricRegistry metric-registry)
            (some? server-id) (.setServerId server-id)
            (some? metric-prefix) (.setMetricPrefix metric-prefix)
            (some? enable-url-metrics?) (.setEnableURLMetrics enable-url-metrics?)
            (some? max-connections-total) (.setMaxConnectionsTotal max-connections-total)
            (some? max-connections-per-route) (.setMaxConnectionsPerRoute max-connections-per-route))
    (JavaClient/createClient client-options)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Map the Ring request onto the Java API

(schema/defn callback-response :- common/Response
  [opts :- common/UserRequestOptions
   callback :- common/ResponseCallbackFn
   response :- common/Response]
  (if callback
    (try
      (callback response)
      (catch Exception e
        {:opts opts :error e}))
    response))

(schema/defn java-content-type->clj :- common/ContentType
  [java-content-type :- (schema/maybe ContentType)]
  (when java-content-type
    {:mime-type (.getMimeType java-content-type)
     :charset   (.getCharset java-content-type)}))

(schema/defn get-response-delivery-delegate :- ResponseDeliveryDelegate
  [opts :- common/UserRequestOptions
   result :- common/ResponsePromise]
  (reify ResponseDeliveryDelegate
    (deliverResponse
      [_ _ orig-encoding body headers status-code reason-phrase content-type callback]
      (->> {:opts                  opts
            :orig-content-encoding orig-encoding
            :status                status-code
            :reason-phrase         reason-phrase
            :headers               (into {} headers)
            :content-type          (java-content-type->clj content-type)
            :body                  body}
           (callback-response opts callback)
           (deliver result)))
    (deliverResponse
      [_ _ e callback]
      (->> {:opts opts :error e}
           (callback-response opts callback)
           (deliver result)))))

(schema/defn clojure-method->java
  [opts :- common/UserRequestOptions]
  (case (:method opts)
    :delete HttpMethod/DELETE
    :get HttpMethod/GET
    :head HttpMethod/HEAD
    :options HttpMethod/OPTIONS
    :patch HttpMethod/PATCH
    :post HttpMethod/POST
    :put HttpMethod/PUT
    :trace HttpMethod/TRACE
    (let [msg (trs "Unsupported request method: {0}" (:method opts))]
      (throw (IllegalArgumentException. ^String msg)))))

(schema/defn url-uri-string->uri :- URI
  [thing :- common/UrlOrUriOrString]
  (cond
    (= (type thing) URL) (.toURI thing)
    (= (type thing) String) (-> (URIBuilder. ^String thing)
                                (.build))
    :else thing))

(schema/defn  parse-url :- URI
  [{:keys [url query-params]}]
  (let [uri (url-uri-string->uri url)]
    (if (nil? query-params)
      uri
      (let [uri-builder (reduce #(.addParameter %1 (key %2) (val %2))
                                (.clearParameters (URIBuilder. ^URI uri))
                                query-params)]
        (.build uri-builder)))))

(schema/defn clojure-response-body-type->java :- ResponseBodyType
  [opts :- common/RequestOptions]
  (case (:as opts)
    :unbuffered-stream ResponseBodyType/UNBUFFERED_STREAM
    :text ResponseBodyType/TEXT
    ResponseBodyType/STREAM))

(schema/defn clojure-compress-request-body-type->java :- CompressType
  [opts :- common/RequestOptions]
  (case (:compress-request-body opts)
    :gzip CompressType/GZIP
    CompressType/NONE))

(defn parse-metric-id
  [opts]
  (when-let [metric-id (:metric-id opts)]
    (into-array (map name metric-id))))

(schema/defn clojure-options->java :- RequestOptions
  [opts :- common/RequestOptions]
  (-> ^URI (parse-url opts)
      RequestOptions.
      (.setAs (clojure-response-body-type->java opts))
      (.setBody (:body opts))
      (.setDecompressBody (clojure.core/get opts :decompress-body true))
      (.setCompressRequestBody (clojure-compress-request-body-type->java opts))
      (.setHeaders (:headers opts))
      (.setMetricId (parse-metric-id opts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn ^:always-validate request-with-client :- common/ResponsePromise
  "Issues an async HTTP request with the specified client and returns a promise
   object to which the value of
   `(callback {:opts _ :status _ :headers _ :body _})` or
   `(callback {:opts _ :error _})` will be delivered.

   When unspecified, `callback` is the identity function.

   opts:

   * :url
   * :method - the HTTP method (:get, :head, :post, :put, :delete, :trace,
               :options, :patch)
   * :headers - a map of headers
   * :body - the body; may be a String or any type supported by clojure's reader
   * :decompress-body - if `true`, an 'accept-encoding' header with a value of
        'gzip, deflate' will be added to the request, and the response will be
        automatically decompressed if it contains a recognized 'content-encoding'
        header.  defaults to `true`.
   * :as - used to control the data type of the response body.  Supported values
       are `:text`, `:stream` and `:unbuffered-stream`. `:text` will return a `String`,
       `:stream` and `:unbuffered-stream` will return an `InputStream`. Note that
       `:stream` holds the full response in memory (i.e. a `ByteArrayInputStream`).
       Use `:unbufferred-stream` for large response bodies or to consume less memory.
       Defaults to `:stream`.
   * :query-params - used to set the query parameters of an http request
   * :metric-id - array of strings or keywords, used to set the metrics to be
       timed for the request."
  ([opts :- common/RawUserRequestOptions
    callback :- common/ResponseCallbackFn
    client :- HttpAsyncClient]
   (request-with-client opts callback client nil nil true))
  ([opts :- common/RawUserRequestOptions
    callback :- common/ResponseCallbackFn
    client :- HttpAsyncClient
    metric-registry :- (schema/maybe MetricRegistry)
    metric-namespace :- (schema/maybe schema/Str)]
   (request-with-client opts callback client metric-registry metric-namespace true))
  ([opts :- common/RawUserRequestOptions
    callback :- common/ResponseCallbackFn
    client :- HttpAsyncClient
    metric-registry :- (schema/maybe MetricRegistry)
    metric-namespace :- (schema/maybe schema/Str)
    enable-url-metrics? :- schema/Bool]
   (let [result (promise)
         defaults {:body nil
                   :decompress-body true
                   :compress-request-body :none
                   :as :stream}
         ^Locale locale (i18n/user-locale)
         ;; lower-case the header names so that we don't end up with
         ;; Accept-Language *AND* accept-language in the headers
         headers (into {"accept-language" (.toLanguageTag locale)}
                       (for [[header value] (:headers opts)]
                         [(str/lower-case header) value]))
         opts (-> (merge defaults opts)
                  (assoc :headers headers))
         java-request-options (clojure-options->java opts)
         java-method (clojure-method->java opts)
         response-delivery-delegate (get-response-delivery-delegate opts result)]
     (JavaClient/requestWithClient java-request-options java-method callback
                                   client response-delivery-delegate metric-registry
                                   metric-namespace
                                   enable-url-metrics?)
     result)))

(schema/defn create-client :- (schema/protocol common/HTTPClient)
  "Creates a client to be used for making one or more HTTP requests.

   opts (base set):

   * :force-redirects - used to set whether or not the client should follow
       redirects on POST or PUT requests. Defaults to false.
   * :follow-redirects - used to set whether or  not the client should follow
       redirects in general. Defaults to true. If set to false, will override
       the :force-redirects setting.
   * :connect-timeout-milliseconds - maximum number of milliseconds that the
       client will wait for a connection to be established.  A value of zero is
       interpreted as infinite.  A negative value for or the absence of this
       option is interpreted as undefined (system default).
   * :socket-timeout-milliseconds - maximum number of milliseconds that the
       client will allow for no data to be available on the socket before
       closing the underlying connection, 'SO_TIMEOUT' in socket terms.  A
       timeout of zero is interpreted as an infinite timeout.  A negative value
       for or the absence of this setting is interpreted as undefined (system
       default).
   * :ssl-protocols - used to set the list of SSL protocols that the client
       could select from when talking to the server. Defaults to 'TLSv1',
       'TLSv1.1', and 'TLSv1.2'.
   * :cipher-suites - used to set the cipher suites that the client could
       select from when talking to the server. Defaults to the complete
       set of suites supported by the underlying language runtime.
   * :metric-registry - a MetricRegistry instance used to collect metrics
       on client requests.

   opts (ssl-specific where only one of the following combinations permitted):

   * :ssl-context - an instance of SSLContext

   OR

   * :ssl-cert - path to a PEM file containing the client cert
   * :ssl-key - path to a PEM file containing the client private key
   * :ssl-ca-cert - path to a PEM file containing the CA cert

   OR

   * :ssl-ca-cert - path to a PEM file containing the CA cert"
  [opts :- common/ClientOptions]
  (let [client (create-default-client opts)
        metric-registry (:metric-registry opts)
        metric-namespace (metrics/build-metric-namespace (:metric-prefix opts) (:server-id opts))
        enable-url-metrics? (clojure.core/get opts :enable-url-metrics? true)]
    (reify common/HTTPClient
      (get [this url] (common/get this url {}))
      (get [this url opts] (common/make-request this url :get opts))
      (head [this url] (common/head this url {}))
      (head [this url opts] (common/make-request this url :head opts))
      (post [this url] (common/post this url {}))
      (post [this url opts] (common/make-request this url :post opts))
      (put [this url] (common/put this url {}))
      (put [this url opts] (common/make-request this url :put opts))
      (delete [this url] (common/delete this url {}))
      (delete [this url opts] (common/make-request this url :delete opts))
      (trace [this url] (common/trace this url {}))
      (trace [this url opts] (common/make-request this url :trace opts))
      (options [this url] (common/options this url {}))
      (options [this url opts] (common/make-request this url :options opts))
      (patch [this url] (common/patch this url {}))
      (patch [this url opts] (common/make-request this url :patch opts))
      (make-request [this url method] (common/make-request this url method {}))
      (make-request [_ url method opts] (request-with-client
                                         (assoc opts :method method :url url)
                                         nil client metric-registry
                                         metric-namespace
                                         enable-url-metrics?))
      (close [_] (.close client))
      (get-client-metric-registry [_] metric-registry)
      (get-client-metric-namespace [_] metric-namespace))))
