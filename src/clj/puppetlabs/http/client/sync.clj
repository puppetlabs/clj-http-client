;; This namespace provides synchronous versions of the request functions
;; defined in puppetlabs.http.client

(ns puppetlabs.http.client.sync
  (:import (org.apache.http.impl.nio.client HttpAsyncClients)
           (org.apache.http.impl.client LaxRedirectStrategy))
  (:require [puppetlabs.http.client.async :as async]
            [schema.core :as schema]
            [puppetlabs.http.client.common :as common])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private utility functions

(defn request-with-client
  [req client]
  (let [{:keys [error] :as resp} @(async/request-with-client req nil client)]
    (if error
      (throw error)
      resp)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn request
  [req]
  (let [{:keys [error] :as resp} @(async/request req nil)]
    (if error
      (throw error)
      resp)))

(schema/defn create-client :- common/HTTPClient
  [opts :- common/ClientOptions]
  (let [client (async/create-default-client opts)]
    (reify common/HTTPClient
      (get [this url] (common/get this url {}))
      (get [_ url opts] (request-with-client (assoc opts :method :get :url url) client))
      (head [this url] (common/head this url {}))
      (head [_ url opts] (request-with-client (assoc opts :method :head :url url) client))
      (post [this url] (common/post this url {}))
      (post [_ url opts] (request-with-client (assoc opts :method :post :url url) client))
      (put [this url] (common/put this url {}))
      (put [_ url opts] (request-with-client (assoc opts :method :put :url url) client))
      (delete [this url] (common/delete this url {}))
      (delete [_ url opts] (request-with-client (assoc opts :method :delete :url url) client))
      (trace [this url] (common/trace this url {}))
      (trace [_ url opts] (request-with-client (assoc opts :method :trace :url url) client))
      (options [this url] (common/options this url {}))
      (options [_ url opts] (request-with-client (assoc opts :method :options :url url) client))
      (patch [this url] (common/patch this url {}))
      (patch [_ url opts] (request-with-client (assoc opts :method :patch :url url) client))
      (close [_] (.close client)))))

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


