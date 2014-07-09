;; This namespace provides synchronous versions of the request functions
;; defined in puppetlabs.http.client

(ns puppetlabs.http.client.sync
  (:require [puppetlabs.http.client.async :as async])
  (:refer-clojure :exclude (get)))

(defn request
  [req]
  (let [{:keys [error] :as resp} @(async/request req nil)]
    (if error
      (throw error)
      resp)))

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


