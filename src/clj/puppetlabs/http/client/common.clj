(ns puppetlabs.http.client.common
  (:import (java.net URL)
           (javax.net.ssl SSLContext)
           (com.codahale.metrics MetricRegistry)
           (clojure.lang IBlockingDeref)
           (java.io InputStream)
           (java.nio.charset Charset)
           (com.puppetlabs.http.client.metrics ClientTimer))
  (:require [schema.core :as schema])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Client Protocol

(defprotocol HTTPClient
  (get [this url] [this url opts])
  (head [this url] [this url opts])
  (post [this url] [this url opts])
  (put [this url] [this url opts])
  (delete [this url] [this url opts])
  (trace [this url] [this url opts])
  (options [this url] [this url opts])
  (patch [this url] [this url opts])
  (make-request [this url method] [this url method opts])
  (close [this])
  (get-client-metric-registry [this])
  (get-client-metric-namespace [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(def ok schema/optional-key)

(def UrlOrString (schema/either schema/Str URL))

(def Headers
  {schema/Str schema/Str})

(def Body
  (schema/maybe (schema/either String InputStream)))

(def BodyType
  (schema/enum :text :stream :unbuffered-stream))

(def CompressType
  (schema/enum :gzip :none))

(def MetricId [(schema/either schema/Str schema/Keyword)])

(def RawUserRequestClientOptions
  "The list of request and client options passed by a user into
  the request function. Allows the user to configure
  both a client and a request."
  {:url                   UrlOrString
   :method                schema/Keyword
   (ok :headers)          Headers
   (ok :body)             Body
   (ok :decompress-body)  schema/Bool
   (ok :compress-request-body) CompressType
   (ok :as)               BodyType
   (ok :query-params)     {schema/Str schema/Str}
   (ok :metric-id)        [schema/Str]

   (ok :ssl-context)      SSLContext
   (ok :ssl-cert)         UrlOrString
   (ok :ssl-key)          UrlOrString
   (ok :ssl-ca-cert)      UrlOrString
   (ok :ssl-protocols)    [schema/Str]
   (ok :cipher-suites) [schema/Str]
   (ok :force-redirects)  schema/Bool
   (ok :follow-redirects) schema/Bool
   (ok :connect-timeout-milliseconds) schema/Int
   (ok :socket-timeout-milliseconds) schema/Int})

(def RawUserRequestOptions
  "The list of request options passed by a user into the
  request function. Allows the user to configure a request."
  {:url                   UrlOrString
   :method                schema/Keyword
   (ok :headers)          Headers
   (ok :body)             Body
   (ok :decompress-body)  schema/Bool
   (ok :compress-request-body) CompressType
   (ok :as)               BodyType
   (ok :query-params)     {schema/Str schema/Str}
   (ok :metric-id)        MetricId})

(def RequestOptions
  "The options from UserRequestOptions that have to do with the
  configuration and settings for an individual request. This is
  everything from UserRequestOptions not included in
  ClientOptions."
  {:url                   UrlOrString
   :method                schema/Keyword
   :headers               Headers
   :body                  Body
   :decompress-body       schema/Bool
   :compress-request-body CompressType
   :as                    BodyType
   (ok :query-params)     {schema/Str schema/Str}
   (ok :metric-id)        MetricId})

(def SslContextOptions
  {:ssl-context SSLContext})

(def SslCaCertOptions
  {:ssl-ca-cert UrlOrString})

(def SslCertOptions
  {:ssl-cert    UrlOrString
   :ssl-key     UrlOrString
   :ssl-ca-cert UrlOrString})

(def SslProtocolOptions
  {(ok :ssl-protocols) [schema/Str]
   (ok :cipher-suites) [schema/Str]})

(def BaseClientOptions
  {(ok :force-redirects) schema/Bool
   (ok :follow-redirects) schema/Bool
   (ok :connect-timeout-milliseconds) schema/Int
   (ok :socket-timeout-milliseconds) schema/Int
   (ok :metric-registry) MetricRegistry
   (ok :server-id) schema/Str
   (ok :metric-prefix) schema/Str
   (ok :enable-url-metrics?) schema/Bool
   (ok :max-connections-total) schema/Int
   (ok :max-connections-per-route) schema/Int})

(def UserRequestOptions
  "A cleaned-up version of RawUserRequestClientOptions, which is formed after
  validating the RawUserRequestClientOptions and merging it with the defaults."
  (schema/either
    (merge RequestOptions BaseClientOptions)
    (merge RequestOptions SslContextOptions SslProtocolOptions BaseClientOptions)
    (merge RequestOptions SslCaCertOptions SslProtocolOptions BaseClientOptions)
    (merge RequestOptions SslCertOptions SslProtocolOptions BaseClientOptions)))

(def ClientOptions
  "The options from UserRequestOptions that are related to the
   instantiation/management of a client. This is everything
   from UserRequestOptions not included in RequestOptions."
  (schema/either
    BaseClientOptions
    (merge SslContextOptions SslProtocolOptions BaseClientOptions)
    (merge SslCertOptions SslProtocolOptions BaseClientOptions)
    (merge SslCaCertOptions SslProtocolOptions BaseClientOptions)))

(def ResponseCallbackFn
  (schema/maybe (schema/pred ifn?)))

(def ResponsePromise
  IBlockingDeref)

(def ContentType
  (schema/maybe {:mime-type schema/Str
                 :charset   (schema/maybe Charset)}))

(def NormalResponse
  {:opts UserRequestOptions
   :orig-content-encoding (schema/maybe schema/Str)
   :body Body
   :headers Headers
   :status schema/Int
   :content-type ContentType})

(def ErrorResponse
  {:opts  UserRequestOptions
   :error Exception})

(def Response
  (schema/either NormalResponse ErrorResponse))

(def HTTPMethod
  (schema/enum :delete :get :head :option :patch :post :put :trace))

(def Metrics
  [ClientTimer])

(def AllMetrics
  {:url Metrics
   :url-and-method Metrics
   :metric-id Metrics})

(def BaseMetricData
  {:metric-name schema/Str
   :count schema/Int
   :mean schema/Num
   :aggregate schema/Num})

(def UrlMetricData
  (assoc BaseMetricData :url schema/Str))

(def UrlAndMethodMetricData
  (assoc UrlMetricData :method schema/Str))

(def MetricIdMetricData
  (assoc BaseMetricData :metric-id [schema/Str]))

(def AllMetricsData
  {:url [UrlMetricData]
   :url-and-method [UrlAndMethodMetricData]
   :metric-id [MetricIdMetricData]})

(def MetricTypes
  (schema/enum :full-response))
