(ns puppetlabs.http.client.common
  (:import (java.net URL)
           (javax.net.ssl SSLContext)
           (org.apache.http.impl.nio.client CloseableHttpAsyncClient)
           (clojure.lang IBlockingDeref)
           (java.io InputStream)
           (java.nio.charset Charset))
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
  (close [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(def ok schema/optional-key)

(def UrlOrString (schema/either schema/Str URL))

;; TODO: replace this with a protocol
(def Client CloseableHttpAsyncClient)

(def Headers
  {schema/Str schema/Str})

(def Body
  (schema/maybe (schema/either String InputStream)))

(def BodyType
  (schema/enum :text :stream))

(def RawUserRequestClientOptions
  "The list of Request and client options passed by a user into
  the request function. Allows the user to configure
  both a client and a request."
  {:url                   UrlOrString
   :method                schema/Keyword
   (ok :headers)          Headers
   (ok :body)             Body
   (ok :decompress-body)  schema/Bool
   (ok :as)               BodyType
   (ok :query-params)     {schema/Str schema/Str}

   (ok :ssl-context)      SSLContext
   (ok :ssl-cert)         UrlOrString
   (ok :ssl-key)          UrlOrString
   (ok :ssl-ca-cert)      UrlOrString
   (ok :ssl-protocols)    [schema/Str]
   (ok :cipher-suites) [schema/Str]
   (ok :force-redirects)  schema/Bool
   (ok :follow-redirects) schema/Bool})

(def RawUserRequestOptions
  "The list of request options passed by a user into the
  request function. Allows the user to configure a request."
  {:url                   UrlOrString
   :method                schema/Keyword
   (ok :headers)          Headers
   (ok :body)             Body
   (ok :decompress-body)  schema/Bool
   (ok :as)               BodyType
   (ok :query-params)     {schema/Str schema/Str}})

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
   :as                    BodyType
   (ok :query-params)     {schema/Str schema/Str}})

(def SslContextOptions
  {:ssl-context SSLContext})

(def SslCaCertOptions
  {:ssl-ca-cert UrlOrString})

(def SslCertOptions
  {:ssl-cert    UrlOrString
   :ssl-key     UrlOrString
   :ssl-ca-cert UrlOrString})

(def SslOptions
  (schema/either {} SslContextOptions SslCertOptions SslCaCertOptions))

(def SslProtocolOptions
  {(schema/optional-key :ssl-protocols) [schema/Str]
   (schema/optional-key :cipher-suites) [schema/Str]})

(def RedirectOptions
  {(schema/optional-key :force-redirects)  schema/Bool
   (schema/optional-key :follow-redirects) schema/Bool})

(def UserRequestOptions
  "A cleaned-up version of RawUserRequestClientOptions, which is formed after
  validating the RawUserRequestClientOptions and merging it with the defaults."
  (schema/either
    (merge RequestOptions RedirectOptions)
    (merge RequestOptions SslContextOptions SslProtocolOptions RedirectOptions)
    (merge RequestOptions SslCaCertOptions SslProtocolOptions RedirectOptions)
    (merge RequestOptions SslCertOptions SslProtocolOptions RedirectOptions)))

(def ClientOptions
  "The options from UserRequestOptions that are related to the
   instantiation/management of a client. This is everything
   from UserRequestOptions not included in RequestOptions."
  (schema/either
    RedirectOptions
    (merge SslContextOptions SslProtocolOptions RedirectOptions)
    (merge SslCertOptions SslProtocolOptions RedirectOptions)
    (merge SslCaCertOptions SslProtocolOptions RedirectOptions)))

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


