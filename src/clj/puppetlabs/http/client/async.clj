;; This namespace is a wrapper around the http client functionality provided
;; by Apache HttpAsyncClient. It allows the use of PEM files for HTTPS configuration.
;;
;; In the options to any request method, an existing SSLContext object can be
;; supplied under :ssl-context. If this is present it will be used. If it's
;; not, the wrapper will attempt to use a set of PEM files stored in
;; (:ssl-cert :ssl-key :ssl-ca-cert) to create the SSLContext. It is also
;; still possible to use an SSLEngine directly, and if this is present under
;; the key :sslengine it will be used before any other options are tried.
;;
;; See the puppetlabs.http.sync namespace for synchronous versions of all
;; these methods.

(ns puppetlabs.http.client.async
  (:import (com.puppetlabs.http.client ClientOptions RequestOptions ResponseBodyType HttpMethod)
           (org.apache.http.impl.nio.client HttpAsyncClients)
           (org.apache.http.client.utils URIBuilder)
           (com.puppetlabs.http.client.impl JavaClient ResponseDeliveryDelegate)
           (org.apache.http.client RedirectStrategy)
           (org.apache.http.impl.client LaxRedirectStrategy DefaultRedirectStrategy)
           (org.apache.http.nio.conn.ssl SSLIOSessionStrategy)
           (org.apache.http.client.config RequestConfig)
           (org.apache.http.nio.client HttpAsyncClient)
           (org.apache.http.entity ContentType))
  (:require [puppetlabs.ssl-utils.core :as ssl]
            [puppetlabs.http.client.common :as common]
            [schema.core :as schema])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private SSL configuration functions

(defn- initialize-ssl-context-from-pems
  [req]
  (-> req
      (assoc :ssl-context (ssl/pems->ssl-context
                            (:ssl-cert req)
                            (:ssl-key req)
                            (:ssl-ca-cert req)))
      (dissoc :ssl-cert :ssl-key :ssl-ca-cert)))

(defn- initialize-ssl-context-from-ca-pem
  [req]
  (-> req
      (assoc :ssl-context (ssl/ca-cert-pem->ssl-context
                            (:ssl-ca-cert req)))
      (dissoc :ssl-ca-cert)))

(defn- configure-ssl-from-pems
  "Configures an SSLEngine in the request starting from a set of PEM files"
  [req]
  (initialize-ssl-context-from-pems req))

(defn- configure-ssl-from-ca-pem
  "Configures an SSLEngine in the request starting from a CA PEM file"
  [req]
  (initialize-ssl-context-from-ca-pem req))

(schema/defn configure-ssl-ctxt :- (schema/either {} common/SslContextOptions)
  "Configures a request map to have an SSLContext. It will use an existing one
  (stored in :ssl-context) if already present, and will fall back to a set of
  PEM files (stored in :ssl-cert, :ssl-key, and :ssl-ca-cert) if those are present.
  If none of these are present this does not modify the request map."
  [opts :- common/SslOptions]
  (cond
    (:ssl-context opts) opts
    (every? opts [:ssl-cert :ssl-key :ssl-ca-cert]) (configure-ssl-from-pems opts)
    (:ssl-ca-cert opts) (configure-ssl-from-ca-pem opts)
    :else opts))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private utility functions

(schema/defn extract-ssl-opts :- common/SslOptions
  [opts :- common/ClientOptions]
  (select-keys opts [:ssl-context :ssl-ca-cert :ssl-cert :ssl-key]))

(schema/defn ^:always-validate ssl-strategy :- SSLIOSessionStrategy
  [ssl-ctxt-opts :- common/SslContextOptions
   ssl-prot-opts :- common/SslProtocolOptions]
  (SSLIOSessionStrategy.
    (:ssl-context ssl-ctxt-opts)
    (if (contains? ssl-prot-opts :ssl-protocols)
      (into-array String (:ssl-protocols ssl-prot-opts))
      ClientOptions/DEFAULT_SSL_PROTOCOLS)
    (if (contains? ssl-prot-opts :cipher-suites)
      (into-array String (:cipher-suites ssl-prot-opts)))
    SSLIOSessionStrategy/BROWSER_COMPATIBLE_HOSTNAME_VERIFIER))

(schema/defn ^:always-validate redirect-strategy :- RedirectStrategy
  [opts :- common/ClientOptions]
  (let [follow-redirects (:follow-redirects opts)
        force-redirects (:force-redirects opts)]
    (cond
      (and (not (nil? follow-redirects)) (not follow-redirects))
        (proxy [RedirectStrategy] []
          (isRedirected [req resp context]
            false)
          (getRedirect [req resp context]
            nil))
        force-redirects
          (LaxRedirectStrategy.)
        :else
          (DefaultRedirectStrategy.))))

(schema/defn request-config :- RequestConfig
  [connect-timeout-milliseconds :- (schema/maybe schema/Int)
   socket-timeout-milliseconds :- (schema/maybe schema/Int)]
  (let [request-config-builder (RequestConfig/custom)]
    (if connect-timeout-milliseconds
      (.setConnectTimeout request-config-builder
                          connect-timeout-milliseconds))
    (if socket-timeout-milliseconds
      (.setSocketTimeout request-config-builder
                         socket-timeout-milliseconds))
    (.build request-config-builder)))

(schema/defn ^:always-validate create-default-client :- common/Client
  [opts :- common/ClientOptions]
  (let [ssl-ctxt-opts   (configure-ssl-ctxt (extract-ssl-opts opts))
        ssl-prot-opts   (select-keys opts [:ssl-protocols :cipher-suites])
        client-builder  (HttpAsyncClients/custom)
        connect-timeout (:connect-timeout-milliseconds opts)
        socket-timeout  (:socket-timeout-milliseconds opts)
        client          (do (when (:ssl-context ssl-ctxt-opts)
                              (.setSSLStrategy client-builder
                                               (ssl-strategy
                                                 ssl-ctxt-opts ssl-prot-opts)))
                            (.setRedirectStrategy client-builder
                                                  (redirect-strategy opts))
                            (if (or connect-timeout socket-timeout)
                              (.setDefaultRequestConfig client-builder
                                                        (request-config
                                                          connect-timeout
                                                          socket-timeout)))
                            (.build client-builder))]
    (.start client)
    client))

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
  [java-content-type]
  (if java-content-type
    {:mime-type (.getMimeType java-content-type)
     :charset   (.getCharset java-content-type)}))

(schema/defn get-response-delivery-delegate :- ResponseDeliveryDelegate
  [opts :- common/UserRequestOptions
   result :- common/ResponsePromise]
  (reify ResponseDeliveryDelegate
    (deliverResponse
      [_ _ orig-encoding body headers status content-type callback]
      (let [response {:opts                  opts
                      :orig-content-encoding orig-encoding
                      :status                status
                      :headers               (into {} headers)
                      :content-type          (java-content-type->clj content-type)
                      :body                  body}
            response (callback-response opts callback response)]
        (deliver result response)))
    (deliverResponse
      [_ _ e]
      (deliver result {:opts opts :error e}))))

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
    :trace HttpMethod/TRACE))

(defn- parse-url
  [{:keys [url query-params]}]
  (if (nil? query-params)
    url
    (let [uri-builder (reduce #(.addParameter %1 (key %2) (val %2))
                              (.clearParameters (URIBuilder. url))
                              query-params)]
      (.build uri-builder))))

(schema/defn clojure-response-body-type->java :- ResponseBodyType
  [opts :- common/RequestOptions]
  (case (:as opts)
    :unbuffered-stream ResponseBodyType/UNBUFFERED_STREAM
    :text ResponseBodyType/TEXT
    ResponseBodyType/STREAM))

(schema/defn clojure-options->java :- RequestOptions
  [opts :- common/RequestOptions]
  (-> (parse-url opts)
      RequestOptions.
      (.setAs (clojure-response-body-type->java opts))
      (.setBody (:body opts))
      (.setDecompressBody (clojure.core/get opts :decompress-body true))
      (.setHeaders (:headers opts))))

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
       are `:text` and `:stream`, which will return a `String` or an
       `InputStream`, respectively.  Defaults to `:stream`.
   * :query-params - used to set the query parameters of an http request"
  [opts :- common/RawUserRequestOptions
   callback :- common/ResponseCallbackFn
   client :- HttpAsyncClient]
  (let [result (promise)
        defaults {:headers         {}
                  :body            nil
                  :decompress-body true
                  :as              :stream}
        opts (merge defaults opts)
        java-request-options (clojure-options->java opts)
        java-method (clojure-method->java opts)
        response-delivery-delegate (get-response-delivery-delegate opts result)]
    (JavaClient/requestWithClient java-request-options java-method callback client response-delivery-delegate)
    result))

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

   opts (ssl-specific where only one of the following combinations permitted):

   * :ssl-context - an instance of SSLContext

   OR

   * :ssl-cert - path to a PEM file containing the client cert
   * :ssl-key - path to a PEM file containing the client private key
   * :ssl-ca-cert - path to a PEM file containing the CA cert

   OR

   * :ssl-ca-cert - path to a PEM file containing the CA cert"
  [opts :- common/ClientOptions]
  (let [client (create-default-client opts)]
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
      (options [this url opts] (common/make-request this url :post opts))
      (patch [this url] (common/patch this url {}))
      (patch [this url opts] (common/make-request this url :patch opts))
      (make-request [this url method] (common/make-request this url method {}))
      (make-request [_ url method opts] (request-with-client (assoc opts :method method :url url) nil client))
      (close [_] (.close client)))))
