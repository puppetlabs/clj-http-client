;; This namespace provides synchronous versions of the request functions
;; defined in puppetlabs.http.client

(ns puppetlabs.http.client.sync
  (:require [puppetlabs.http.client.async :as async]
            [schema.core :as schema]
            [puppetlabs.http.client.common :as common])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private utility functions

(schema/defn extract-client-opts :- common/ClientOptions
  [opts :- common/RawUserRequestClientOptions]
  (select-keys opts [:ssl-context :ssl-ca-cert :ssl-cert :ssl-key
                     :ssl-protocols :cipher-suites
                     :force-redirects :follow-redirects
                     :connect-timeout-milliseconds
                     :socket-timeout-milliseconds]))

(schema/defn extract-request-opts :- common/RawUserRequestOptions
  [opts :- common/RawUserRequestClientOptions]
  (select-keys opts [:url :method :headers :body :decompress-body :as :query-params]))

(defn request-with-client
  [req client metric-registry]
  (let [{:keys [error] :as resp} @(async/request-with-client req nil client metric-registry)]
    (if error
      (throw error)
      resp)))

(defn get-client-metrics
  ([metric-registry]
   (async/get-client-metrics metric-registry))
  ([metric-registry metric-filter]
   (async/get-client-metrics metric-registry metric-filter)))

(defn get-client-metrics-data
  ([metric-registry]
   (async/get-client-metrics-data metric-registry))
  ([metric-registry metric-filter]
   (async/get-client-metrics-data metric-registry metric-filter)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn request
  [req]
  (with-open [client (async/create-default-client (extract-client-opts req))]
    (request-with-client (extract-request-opts req) client nil)))

(schema/defn create-client :- (schema/protocol common/HTTPClient)
  [opts :- common/ClientOptions]
  (let [client (async/create-default-client opts)
        metric-registry (:metric-registry opts)]
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
      (make-request [_ url method opts] (request-with-client
                                         (assoc opts :method method :url url)
                                         client metric-registry))
      (close [_] (.close client))
      (get-client-metrics [_] (get-client-metrics metric-registry))
      (get-client-metrics [_ metric-filter] (get-client-metrics metric-registry metric-filter))
      (get-client-metrics-data [_] (get-client-metrics-data metric-registry))
      (get-client-metrics-data [_ metric-filter] (get-client-metrics-data metric-registry metric-filter)))))

(defn get
  "Issue a synchronous HTTP GET request. This will raise an exception if an
  error is returned."
  ([url] (get url {}))
  ([url opts] (request (assoc opts :method :get :url url))))

(defn head
  "Issue a synchronous HTTP head request. This will raise an exception if an
  error is returned."
  ([url] (head url {}))
  ([url opts] (request (assoc opts :method :head :url url))))

(defn post
  "Issue a synchronous HTTP POST request. This will raise an exception if an
  error is returned."
  ([url] (post url {}))
  ([url opts] (request (assoc opts :method :post :url url))))

(defn put
  "Issue a synchronous HTTP PUT request. This will raise an exception if an
  error is returned."
  ([url] (put url {}))
  ([url opts] (request (assoc opts :method :put :url url))))

(defn delete
  "Issue a synchronous HTTP DELETE request. This will raise an exception if an
  error is returned."
  ([url] (delete url {}))
  ([url opts] (request (assoc opts :method :delete :url url))))

(defn trace
  "Issue a synchronous HTTP TRACE request. This will raise an exception if an
  error is returned."
  ([url] (trace url {}))
  ([url opts] (request (assoc opts :method :trace :url url))))

(defn options
  "Issue a synchronous HTTP OPTIONS request. This will raise an exception if an
  error is returned."
  ([url] (options url {}))
  ([url opts] (request (assoc opts :method :options :url url))))

(defn patch
  "Issue a synchronous HTTP PATCH request. This will raise an exception if an
  error is returned."
  ([url] (patch url {}))
  ([url opts] (request (assoc opts :method :patch :url url))))
