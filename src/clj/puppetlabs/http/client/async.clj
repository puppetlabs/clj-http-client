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
  (:import (com.puppetlabs.http.client ClientOptions RequestOptions ResponseBodyType HttpMethod)
           (com.puppetlabs.http.client.impl JavaClient ResponseDeliveryDelegate Metrics Metrics$MetricType)
           (org.apache.http.client.utils URIBuilder)
           (org.apache.http.nio.client HttpAsyncClient)
           (com.codahale.metrics Timer)
           (java.util.concurrent TimeUnit))
  (:require [puppetlabs.http.client.common :as common]
            [schema.core :as schema])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private utility functions

(schema/defn ^:always-validate create-default-client :- HttpAsyncClient
  [{:keys [ssl-context ssl-ca-cert ssl-cert ssl-key ssl-protocols cipher-suites
           follow-redirects force-redirects connect-timeout-milliseconds
           socket-timeout-milliseconds]}:- common/ClientOptions]
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
            (.setSocketTimeoutMilliseconds socket-timeout-milliseconds))
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
      (->> {:opts                  opts
            :orig-content-encoding orig-encoding
            :status                status
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
    (throw (IllegalArgumentException. (format "Unsupported request method: %s" (:method opts))))))

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

(defn parse-metric-id
  [opts]
  (when-let [metric-id (:metric-id opts)]
    (into-array metric-id)))

(schema/defn clojure-options->java :- RequestOptions
  [opts :- common/RequestOptions]
  (-> (parse-url opts)
      RequestOptions.
      (.setAs (clojure-response-body-type->java opts))
      (.setBody (:body opts))
      (.setDecompressBody (clojure.core/get opts :decompress-body true))
      (.setHeaders (:headers opts))
      (.setMetricId (parse-metric-id opts))))

(schema/defn get-mean :- schema/Num
  [timer :- Timer]
  (->> timer
       .getSnapshot
       .getMean
       (.toMillis TimeUnit/NANOSECONDS)))

(defn get-metric-data
  [timer metric-id]
  (let [count (.getCount timer)
        mean (get-mean timer)
        aggregate (* count mean)]
    {:count count
     :mean mean
     :aggregate aggregate
     :metric-id metric-id}))

(defn get-metrics-data
  [timers]
  (reduce (fn [acc [metric-id timer]]
            (assoc acc metric-id (get-metric-data timer metric-id)))
          {} timers))

(schema/defn ^:always-validate get-client-metrics :- (schema/maybe common/Metrics)
  "Returns the http client-specific metrics from the metric registry."
  ([metric-registry :- common/OptionalMetricRegistry]
   (when metric-registry
     (into {} (Metrics/getClientMetrics metric-registry))))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-filter :- common/MetricFilter]
   (when metric-registry
     (cond
       (:method metric-filter) (into {} (Metrics/getClientMetricsWithUrlAndMethod
                                         metric-registry
                                         (:url metric-filter)
                                         (:method metric-filter)
                                         Metrics$MetricType/BYTES_READ))
       (:url metric-filter) (into {} (Metrics/getClientMetricsWithUrl
                                      metric-registry
                                      (:url metric-filter)
                                      Metrics$MetricType/BYTES_READ))
       (:metric-id metric-filter) (into {} (Metrics/getClientMetricsWithMetricId
                                            metric-registry
                                            (into-array (:metric-id metric-filter)) Metrics$MetricType/BYTES_READ))
       :else (throw (IllegalArgumentException. "Not an allowed metric filter."))))))

(schema/defn ^:always-validate get-client-metrics-data :- (schema/maybe common/MetricsData)
  "Returns a map of metric-id to metric data summary."
  ([metric-registry :- common/OptionalMetricRegistry]
   (when metric-registry
     (let [timers (get-client-metrics metric-registry)]
       (get-metrics-data timers))))
  ([metric-registry :- common/OptionalMetricRegistry
    metric-filter :- common/MetricFilter]
   (when metric-registry
     (let [timers (get-client-metrics metric-registry metric-filter)]
       (get-metrics-data timers)))))

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
  ([opts :- common/RawUserRequestOptions
    callback :- common/ResponseCallbackFn
    client :- HttpAsyncClient]
    (request-with-client opts callback client nil))
  ([opts :- common/RawUserRequestOptions
    callback :- common/ResponseCallbackFn
    client :- HttpAsyncClient
    metric-registry :- common/OptionalMetricRegistry]
   (let [result (promise)
         defaults {:headers {}
                   :body nil
                   :decompress-body true
                   :as :stream}
         opts (merge defaults opts)
         java-request-options (clojure-options->java opts)
         java-method (clojure-method->java opts)
         response-delivery-delegate (get-response-delivery-delegate opts result)]
     (JavaClient/requestWithClient java-request-options java-method callback
                                   client response-delivery-delegate metric-registry)
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

   opts (ssl-specific where only one of the following combinations permitted):

   * :ssl-context - an instance of SSLContext

   OR

   * :ssl-cert - path to a PEM file containing the client cert
   * :ssl-key - path to a PEM file containing the client private key
   * :ssl-ca-cert - path to a PEM file containing the CA cert

   OR

   * :ssl-ca-cert - path to a PEM file containing the CA cert"
  ([opts :- common/ClientOptions]
   (create-client opts nil))
  ([opts :- common/ClientOptions
    metric-registry :- common/OptionalMetricRegistry]
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
       (options [this url opts] (common/make-request this url :options opts))
       (patch [this url] (common/patch this url {}))
       (patch [this url opts] (common/make-request this url :patch opts))
       (make-request [this url method] (common/make-request this url method {}))
       (make-request [_ url method opts] (request-with-client
                                          (assoc opts :method method :url url)
                                          nil client metric-registry))
       (close [_] (.close client))
       (get-client-metrics [_] (get-client-metrics metric-registry))
       (get-client-metrics [_ metric-filter] (get-client-metrics metric-registry metric-filter))
       (get-client-metrics-data [_] (get-client-metrics-data metric-registry))
       (get-client-metrics-data [_ metric-filter] (get-client-metrics-data metric-registry metric-filter))))))
