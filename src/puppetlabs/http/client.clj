;; This namespace is a thin wrapper around the http client functionality provided
;; by org.httpkit.client.  The wrapper currently serves two purposes:
;;
;; 1. Support simple configuration options (:ssl-cert, :ssl-key, :ssl-ca-cert)
;;    for issuing HTTPS requests using PEM files on disk for SSL configuration
;; 2. `request` wrapper function makes all requests appear to be synchronous,
;;    for an API that is more compatible with the clj-http synchronous API that
;;    we've been using elsewhere.
;;
;; Would like to expose a `request-async` function soon, to allow callers to
;; use async functionality if they prefer.
(ns puppetlabs.http.client
  (:require [org.httpkit.client :as http]
            [puppetlabs.kitchensink.ssl :as ssl])
  (:refer-clojure :exclude (get)))

(defn initialize-ssl
  [req]
  (-> req
    (assoc :sslengine (.createSSLEngine (:ssl-context req)))
    (dissoc :ssl-context)))

(defn configure-ssl
  [req]
  (if-not (every? #(req %) #{:ssl-cert :ssl-key :ssl-ca-cert})
    req
    (let [ssl-context (ssl/pems->ssl-context
                        (:ssl-cert req)
                        (:ssl-key req)
                        (:ssl-ca-cert req))]
      (-> req
        (assoc :ssl-context ssl-context)
        (initialize-sslengine)
        (dissoc :ssl-cert :ssl-key :ssl-ca-cert)))))

(defn configure-output
  [req]
  (if (:as req)
    req
    (assoc req :as :text)))

(defn configure-req
  [req]
  (-> req
    configure-ssl
    configure-output))

(defn request
  [req]
  (let [{:keys [status headers body error] :as resp}
          @(http/request (configure-req req) nil)]
    (if error
      (throw error)
      resp)))

(defn check-url! [url]
  (when (nil? url)
     (throw (IllegalArgumentException. "Host URL cannot be nil"))))

(defn get
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (check-url! url)
  (request (merge req {:method :get :url url})))

(defn head
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (check-url! url)
  (request (merge req {:method :head :url url})))

(defn post
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (check-url! url)
  (request (merge req {:method :post :url url})))

(defn put
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (check-url! url)
  (request (merge req {:method :put :url url})))

(defn delete
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (check-url! url)
  (request (merge req {:method :delete :url url})))

(defn options
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (check-url! url)
  (request (merge req {:method :options :url url})))

(defn patch
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (check-url! url)
  (request (merge req {:method :patch :url url})))
