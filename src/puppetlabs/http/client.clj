;; This namespace is a thin wrapper around the http client functionality provided
;; by org.httpkit.client. It makes SSL configuration more flexible, and allows
;; the use of PEM files.
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

(ns puppetlabs.http.client
  (:require [org.httpkit.client :as http]
            [puppetlabs.kitchensink.ssl :as ssl])
  (:refer-clojure :exclude (get)))

;; SSL configuration functions

(defn- initialize-ssl-context-from-pems
  [req]
  (-> req
    (assoc :ssl-context (ssl/pems->ssl-context
                          (:ssl-cert req)
                          (:ssl-key req)
                          (:ssl-ca-cert req)))
    (dissoc :ssl-cert :ssl-key :ssl-ca-cert)))

(defn- configure-ssl-from-context
  "Configures an SSLEngine in the request starting from an SSLContext"
  [req]
  (-> req
    (assoc :sslengine (.createSSLEngine (:ssl-context req)))
    (dissoc :ssl-context)))

(defn- configure-ssl-from-pems
  "Configures an SSLEngine in the request starting from a set of PEM files"
  [req]
  (-> req
    initialize-ssl-context-from-pems
    configure-ssl-from-context))

(defn configure-ssl
  "Configures a request map to have an SSLEngine. It will use an existing one
  if already present, , then use an SSLContext (stored in :ssl-context) if
  that is present, and will fall back to a set of PEM files (stored in
  :ssl-cert, :ssl-key, and :ssl-ca-cert) if those are present. If none of
  these are present this does not modify the request map."
  [req]
  (cond
    (:sslengine req) req
    (:ssl-context req) (configure-ssl-from-context req)
    (every? (partial req) [:ssl-cert :ssl-key :ssl-ca-cert]) (configure-ssl-from-pems req)
    :else req))

(defn- check-url! [url]
  (when (nil? url)
     (throw (IllegalArgumentException. "Host URL cannot be nil"))))

(defn request
  [opts callback]
  (http/request (configure-ssl opts callback)))

(defn- wrap-with-ssl-config
  [method]
  (fn wrapped-fn
    ([url] (wrapped-fn url nil {}))
    ([url callback] (wrapped-fn url callback {}))
    ([url callback opts]
     (check-url! url)
     (method url (configure-ssl opts) callback))))

(def ^{:arglists '([url] [url callback] [url callback opts])} get
  "Issue an async HTTP GET request."
  (wrap-with-ssl-config http/get))

(def ^{:arglists '([url] [url callback] [url callback opts])} head
  "Issue an async HTTP HEAD request."
  (wrap-with-ssl-config http/head))

(def ^{:arglists '([url] [url callback] [url callback opts])} post
  "Issue an async HTTP POST request."
  (wrap-with-ssl-config http/post))

(def ^{:arglists '([url] [url callback] [url callback opts])} put
  "Issue an async HTTP PUT request."
  (wrap-with-ssl-config http/put))

(def ^{:arglists '([url] [url callback] [url callback opts])} delete
  "Issue an async HTTP DELETE request."
  (wrap-with-ssl-config http/delete))

(def ^{:arglists '([url] [url callback] [url callback opts])} options
  "Issue an async HTTP OPTIONS request."
  (wrap-with-ssl-config http/options))

(def ^{:arglists '([url] [url callback] [url callback opts])} patch
  "Issue an async HTTP PATCH request."
  (wrap-with-ssl-config http/patch))
