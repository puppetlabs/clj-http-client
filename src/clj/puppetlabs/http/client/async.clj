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
  (:import (com.puppetlabs.http.client HttpMethod HttpClientException)
           (org.apache.http.nio.client HttpAsyncClient)
           (org.apache.http.impl.nio.client HttpAsyncClients)
           (org.apache.http.client.methods HttpGet HttpHead HttpPost HttpPut HttpTrace HttpDelete HttpOptions HttpPatch)
           (org.apache.http.client.utils URIBuilder)
           (org.apache.http.concurrent FutureCallback)
           (org.apache.http.message BasicHeader)
           (org.apache.http Consts Header)
           (org.apache.http.nio.entity NStringEntity)
           (org.apache.http.entity InputStreamEntity ContentType)
           (java.io InputStream)
           (com.puppetlabs.http.client.impl Compression)
           (org.apache.http.client RedirectStrategy)
           (org.apache.http.impl.client LaxRedirectStrategy DefaultRedirectStrategy))
  (:require [puppetlabs.certificate-authority.core :as ssl]
            [clojure.string :as str]
            [puppetlabs.kitchensink.core :as ks]
            [puppetlabs.http.client.common :as common]
            [schema.core :as schema]
            [clojure.tools.logging :as log])
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

(schema/defn configure-ssl :- (schema/either {} common/SslContextOptions)
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

(defn- add-accept-encoding-header
  [decompress-body? headers]
  (if (and decompress-body?
           (not (contains? headers "accept-encoding")))
    (assoc headers "accept-encoding"
                   (BasicHeader. "accept-encoding" "gzip, deflate"))
    headers))

(defn- add-content-type-header
  [content-type headers]
  (if content-type
    (assoc headers "content-type" (BasicHeader. "Content-Type"
                                                (.toString content-type)))
    headers))

(defn- prepare-headers
  [{:keys [headers decompress-body]} content-type]
  (->> headers
       (reduce
         (fn [acc [k v]]
           (assoc acc (str/lower-case k) (BasicHeader. k v)))
         {})
       (add-accept-encoding-header decompress-body)
       (add-content-type-header content-type)
       vals
       (into-array Header)))

(defn- parse-url
  [url query-params]
  (if (nil? query-params)
    url
    (let [uri-builder (reduce #(.addParameter %1 (key %2) (val %2))
                              (.clearParameters (URIBuilder. url))
                              query-params)]
      (.build uri-builder))))

(defn- content-type
  [{:keys [headers]}]
  (if-let [content-type-value (some #(when (= "content-type"
                                           (clojure.string/lower-case (key %)))
                                       (val %))
                                    headers)]
    (let [content-type (ContentType/parse content-type-value)]
      (if (.getCharset content-type)
        content-type
        (ContentType/create (.getMimeType content-type) Consts/UTF_8)))))

(defn- coerce-opts
  [{:keys [url body query-params] :as opts}]
  (let [url          (parse-url url query-params)
        content-type (content-type opts)]
    {:url     url
     :method  (clojure.core/get opts :method :get)
     :headers (prepare-headers opts content-type)
     :body    (cond
                (string? body) (if content-type
                                 (NStringEntity. body content-type)
                                 (NStringEntity. body))
                (instance? InputStream body) (InputStreamEntity. body)
                :else body)}))

(defn- construct-request
  [method url]
  (condp = method
    :get    (HttpGet. url)
    :head   (HttpHead. url)
    :post   (HttpPost. url)
    :put    (HttpPut. url)
    :delete (HttpDelete. url)
    :trace  (HttpTrace. url)
    :options (HttpOptions. url)
    :patch  (HttpPatch. url)
    (throw (IllegalArgumentException. (format "Unsupported request method: %s" method)))))

(defn- get-resp-headers
  [http-response]
  (reduce
    (fn [acc h]
      (assoc acc (.. h getName toLowerCase) (.getValue h)))
    {}
    (.getAllHeaders http-response)))

(defmulti decompress (fn [resp] (get-in resp [:headers "content-encoding"])))

(defmethod decompress "gzip"
  [resp]
  (-> resp
      (ks/dissoc-in [:headers "content-encoding"])
      (update-in [:body] #(Compression/gunzip %))))

(defmethod decompress "deflate"
  [resp]
  (-> resp
     (ks/dissoc-in [:headers "content-encoding"])
     (update-in [:body] #(Compression/inflate %))))

(defmethod decompress nil
  [resp]
  resp)

(defn- parse-content-type
  [content-type-header]
  (if (empty? content-type-header)
    nil
    (let [content-type (ContentType/parse content-type-header)]
      {:mime-type (.getMimeType content-type)
       :charset   (.getCharset content-type)})))

(defmulti coerce-body-type (fn [resp] (get-in resp [:opts :as])))

(defmethod coerce-body-type :text
  [resp]
  (let [charset (or (get-in resp [:content-type-params :charset] "UTF-8"))]
    (assoc resp :body (slurp (:body resp) :encoding charset))))

(defn- response-map
  [opts http-response]
  (let [headers       (get-resp-headers http-response)
        orig-encoding (headers "content-encoding")]
    {:opts                  opts
     :orig-content-encoding orig-encoding
     :status                (.. http-response getStatusLine getStatusCode)
     :headers               headers
     :content-type          (parse-content-type (headers "content-type"))
     :body                  (when-let [entity (.getEntity http-response)]
                              (.getContent entity))}))

(schema/defn error-response :- common/ErrorResponse
  [opts :- common/UserRequestOptions
   e :- Exception]
  {:opts opts
   :error e})

(schema/defn callback-response :- common/Response
  [opts :- common/UserRequestOptions
   callback :- common/ResponseCallbackFn
   response :- common/Response]
  (if callback
    (try
      (callback response)
      (catch Exception e
        (error-response opts e)))
    response))

(schema/defn deliver-result
  [client :- common/Client
   result :- common/ResponsePromise
   opts :- common/UserRequestOptions
   callback :- common/ResponseCallbackFn
   response :- common/Response]
  (try
    (deliver result (callback-response opts callback response))
    (finally
      (if (not (:persistent opts))
        (.close client)))))

(schema/defn future-callback
  [client :- common/Client
   result :- common/ResponsePromise
   opts :- common/RequestOptions
   callback :- common/ResponseCallbackFn]
  (reify FutureCallback
    (completed [this http-response]
      (try
        (let [response (cond-> (response-map opts http-response)
                               (:decompress-body opts) (decompress)
                               (not= :stream (:as opts)) (coerce-body-type))]
          (deliver-result client result opts callback response))
        (catch Exception e
          (log/warn e "Error when delivering response")
          (deliver-result client result opts callback
                          (error-response opts e)))))
    (failed [this e]
      (deliver-result client result opts callback
                      (error-response opts e)))
    (cancelled [this]
      (deliver-result client result opts callback
                      (error-response
                        opts
                        (HttpClientException. "Request cancelled"))))))

(schema/defn extract-client-opts :- common/ClientOptions
  [opts :- common/UserRequestOptions]
  (select-keys opts [:ssl-context :ssl-ca-cert :ssl-cert :ssl-key
                     :force-redirects :follow-redirects]))

(schema/defn extract-ssl-opts :- common/SslOptions
  [opts :- common/ClientOptions]
  (select-keys opts [:ssl-context :ssl-ca-cert :ssl-cert :ssl-key]))

(schema/defn extract-request-opts :- common/RequestOptions
  [opts :- common/UserRequestOptions]
  (select-keys opts [:url :method :headers :body :decompress-body :as :persistent :query-params]))

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

(schema/defn ^:always-validate create-default-client :- common/Client
  [opts :- common/ClientOptions]
  (let [configured-opts (configure-ssl (extract-ssl-opts opts))
        client-builder  (HttpAsyncClients/custom)
        client          (do (when (:ssl-context configured-opts)
                              (.setSSLContext client-builder
                                              (:ssl-context configured-opts)))
                            (.setRedirectStrategy client-builder
                                                  (redirect-strategy opts))
                            (.build client-builder))]
    (.start client)
    client))

(schema/defn ^:always-validate request-with-client :- common/ResponsePromise
  [opts :- common/RawUserRequestClientOptions
   callback :- common/ResponseCallbackFn
   client]
  (let [persistent (not (nil? client))
        defaults {:headers         {}
                  :body            nil
                  :decompress-body true
                  :as              :stream}
        opts (assoc (merge defaults opts) :persistent persistent)
        client-opts (extract-client-opts opts)
        request-opts (extract-request-opts opts)
        client (or client (create-default-client client-opts))
        {:keys [method url body] :as coerced-opts} (coerce-opts opts)
        request (construct-request method url)
        result (promise)]
    (.setHeaders request (:headers coerced-opts))
    (when body
      (.setEntity request body))
    (.execute client request
              (future-callback client result request-opts callback))
    result))

(schema/defn ^:always-validate request-with-reified-client :- common/ResponsePromise
  [opts :- common/RawUserRequestOptions
   callback :- common/ResponseCallbackFn
   client]
  (request-with-client opts callback client))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn ^:always-validate request :- common/ResponsePromise
  "Issues an async HTTP request and returns a promise object to which the value
  of `(callback {:opts _ :status _ :headers _ :body _})` or
     `(callback {:opts _ :error _})` will be delivered.

  When unspecified, `callback` is the identity function.

  Request options:

  * :url
  * :method - the HTTP method (:get, :head, :post, :put, :delete, :options, :patch
  * :headers - a map of headers
  * :body - the body; may be a String or any type supported by clojure's reader
  * :decompress-body - if `true`, an 'accept-encoding' header with a value of
       'gzip, deflate' will be added to the request, and the response will be
       automatically decompressed if it contains a recognized 'content-encoding'
       header.  defaults to `true`.
  * :as - used to control the data type of the response body.  Supported values are
      `:text` and `:stream`, which will return a `String` or an `InputStream`,
      respectively.  Defaults to `:stream`.
  * :query-params - used to set the query parameters of an http request
  * :force-redirects - used to set whether or not the client should follow
      redirects on POST or PUT requests. Defaults to false.
  * :follow-redirects - used to set whether or  not the client should follow
      redirects in general. Defaults to true. If set to false, will override
      the :force-redirects setting.

  SSL options:

  * :ssl-context - an instance of SSLContext

  OR

  * :ssl-cert - path to a PEM file containing the client cert
  * :ssl-key - path to a PEM file containing the client private key
  * :ssl-ca-cert - path to a PEM file containing the CA cert"
  ([opts :- common/RawUserRequestClientOptions]
   (request opts nil))
  ([opts :- common/RawUserRequestClientOptions
    callback :- common/ResponseCallbackFn]
   (request-with-client opts callback nil)))

(schema/defn create-client :- common/HTTPClient
  [opts :- common/ClientOptions]
  (let [configured-opts (configure-ssl (extract-ssl-opts opts))
        client-builder  (HttpAsyncClients/custom)
        client          (do (when (:ssl-context configured-opts)
                              (.setSSLContext client-builder
                                              (:ssl-context configured-opts)))
                            (.setRedirectStrategy client-builder
                                                  (redirect-strategy opts))
                            (.build client-builder))]
    (.start client)
    (reify common/HTTPClient
      (get [this url] (common/get this url {}))
      (get [_ url opts] (request-with-reified-client (assoc opts :method :get :url url) nil client))
      (head [this url] (common/head this url {}))
      (head [_ url opts] (request-with-reified-client (assoc opts :method :head :url url) nil client))
      (post [this url] (common/post this url {}))
      (post [_ url opts] (request-with-reified-client (assoc opts :method :post :url url) nil client))
      (put [this url] (common/put this url {}))
      (put [_ url opts] (request-with-reified-client (assoc opts :method :put :url url) nil client))
      (delete [this url] (common/delete this url {}))
      (delete [_ url opts] (request-with-reified-client (assoc opts :method :delete :url url) nil client))
      (trace [this url] (common/trace this url {}))
      (trace [_ url opts] (request-with-reified-client (assoc opts :method :trace :url url) nil client))
      (options [this url] (common/options this url {}))
      (options [_ url opts] (request-with-reified-client (assoc opts :method :options :url url) nil client))
      (patch [this url] (common/patch this url {}))
      (patch [_ url opts] (request-with-reified-client (assoc opts :method :patch :url url) nil client))
      (close [_] (.close client)))))

(defn get
  "Issue an asynchronous HTTP GET request. This will raise an exception if an
  error is returned."
  ([url] (get url {}))
  ([url opts] (request (assoc opts :method :get :url url))))

(defn head
  "Issue an asynchronous HTTP head request. This will raise an exception if an
  error is returned."
  ([url] (head url {}))
  ([url opts] (request (assoc opts :method :head :url url))))

(defn post
  "Issue an asynchronous HTTP POST request. This will raise an exception if an
  error is returned."
  ([url] (post url {}))
  ([url opts] (request (assoc opts :method :post :url url))))

(defn put
  "Issue an asynchronous HTTP PUT request. This will raise an exception if an
  error is returned."
  ([url] (put url {}))
  ([url opts] (request (assoc opts :method :put :url url))))

(defn delete
  "Issue an asynchronous HTTP DELETE request. This will raise an exception if an
  error is returned."
  ([url] (delete url {}))
  ([url opts] (request (assoc opts :method :delete :url url))))

(defn trace
  "Issue an asynchronous HTTP TRACE request. This will raise an exception if an
  error is returned."
  ([url] (trace url {}))
  ([url opts] (request (assoc opts :method :trace :url url))))

(defn options
  "Issue an asynchronous HTTP OPTIONS request. This will raise an exception if an
  error is returned."
  ([url] (options url {}))
  ([url opts] (request (assoc opts :method :options :url url))))

(defn patch
  "Issue an asynchronous HTTP PATCH request. This will raise an exception if an
  error is returned."
  ([url] (patch url {}))
  ([url opts] (request (assoc opts :method :patch :url url))))
