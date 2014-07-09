(ns puppetlabs.http.client.schemas
  (:import (java.net URL)
           (javax.net.ssl SSLContext)
           (org.apache.http.impl.nio.client CloseableHttpAsyncClient)
           (clojure.lang IBlockingDeref)
           (java.io InputStream)
           (java.nio.charset Charset))
  (:require [schema.core :as schema]))

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

(def RawUserRequestOptions
  {:url                   UrlOrString
   :method                schema/Keyword
   (ok :headers)          Headers
   (ok :body)             Body
   (ok :decompress-body)  schema/Bool
   (ok :as)               BodyType

   (ok :ssl-context)      SSLContext
   (ok :ssl-cert)         UrlOrString
   (ok :ssl-key)          UrlOrString
   (ok :ssl-ca-cert)      UrlOrString})

(def RequestOptions
  {:url             UrlOrString
   :method          schema/Keyword
   :headers         Headers
   :body            Body
   :decompress-body schema/Bool
   :as              BodyType})

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

(def UserRequestOptions
  (schema/either
    RequestOptions
    (merge RequestOptions SslContextOptions)
    (merge RequestOptions SslCaCertOptions)
    (merge RequestOptions SslCertOptions)))

(def ClientOptions
  SslOptions)

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

