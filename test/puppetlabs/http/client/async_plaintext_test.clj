(ns puppetlabs.http.client.async-plaintext-test
  (:import (com.puppetlabs.http.client Async RequestOptions ClientOptions)
           (org.apache.http.impl.nio.client HttpAsyncClients)
           (java.net URI SocketTimeoutException ServerSocket)
           (java.util Locale)
           (java.util.concurrent CountDownLatch TimeUnit))
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.test-common :refer :all]
            [puppetlabs.i18n.core :as i18n]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.testutils.webserver :as testwebserver]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.common :as common]
            [puppetlabs.http.client.async :as async]
            [schema.test :as schema-test]
            [ring.middleware.cookies :refer [wrap-cookies]]))

(use-fixtures :once schema-test/validate-schemas)

(defn app
  [_]
  {:status 200
   :body "Hello, World!"})

(defn app-with-empty-content-type
  [_]
  {:headers {"content-type" ""}
   :status 200
   :body "Hello, World!"})

(defn app-with-language-header-echo
  [{{:strs [accept-language]} :headers}]
  {:status 200
   :body (str accept-language)})

(tk/defservice test-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler app "/hello")
        context))

(defn cookie-handler
  [_]
  {:status 200
   :body "cookie has been set"
   :cookies {"session_id" {:value "session-id-hash"}}})

(defn check-cookie-handler
  [req]
  (if (empty? (get req :cookies))
    {:status 400
     :body "cookie has not been set"}
    {:status 200
     :body "cookie has been set"}))

(tk/defservice test-cookie-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler (wrap-cookies cookie-handler) "/cookietest")
        (add-ring-handler (wrap-cookies check-cookie-handler) "/cookiecheck")
        context))

(deftest persistent-async-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
    [jetty9/jetty9-service test-web-service]
    {:webserver {:port 10000}}
    (testing "java async client"
      (let [request-options (RequestOptions. (URI. "http://localhost:10000/hello/"))
            client-options (ClientOptions.)
            client (Async/createClient client-options)]
        (testing "HEAD request with persistent async client"
          (let [response (.head client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= nil (.getBody (.deref response))))))
        (testing "GET request with persistent async client"
          (let [response (.get client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "POST request with persistent async client"
          (let [response (.post client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "PUT request with persistent async client"
          (let [response (.put client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "DELETE request with persistent async client"
          (let [response (.delete client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "TRACE request with persistent async client"
          (let [response (.trace client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "OPTIONS request with persistent async client"
          (let [response (.options client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "PATCH request with persistent async client"
          (let [response (.patch client request-options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "client closes properly"
          (.close client)
          (is (thrown? IllegalStateException
                       (.get client request-options))))))
    (testing "clojure async client"
      (let [client (async/create-client {})]
        (testing "HEAD request with persistent async client"
          (let [response (common/head client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= nil (:body @response)))))
        (testing "GET request with persistent async client"
          (let [response (common/get client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "POST request with persistent async client"
          (let [response (common/post client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "PUT request with persistent async client"
          (let [response (common/put client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "DELETE request with persistent async client"
          (let [response (common/delete client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "TRACE request with persistent async client"
          (let [response (common/trace client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "OPTIONS request with persistent async client"
          (let [response (common/options client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "PATCH request with persistent async client"
          (let [response (common/patch client "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "GET request via request function with persistent async client"
          (let [response (common/make-request client "http://localhost:10000/hello/" :get)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "Bad verb request via request function with persistent async client"
          (is (thrown? IllegalArgumentException
                       (common/make-request client
                                            "http://localhost:10000/hello/"
                                            :bad))))
        (testing "client closes properly"
          (common/close client)
          (is (thrown? IllegalStateException
                       (common/get client
                                   "http://localhost:10000/hello/")))))))))

(deftest java-api-cookie-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-cookie-service]
      {:webserver {:port 10000}}
      (let [client (Async/createClient (ClientOptions.))]
        (testing "Set a cookie using Java API"
          (let [response (.get client (RequestOptions. (URI. "http://localhost:10000/cookietest")))]
            (is (= 200 (.getStatus (.deref response))))))
        (testing "Check if cookie still exists"
          (let [response (.get client (RequestOptions. (URI. "http://localhost:10000/cookiecheck")))]
            (is (= 200 (.getStatus (.deref response))))))))))

(deftest clj-api-cookie-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-cookie-service]
      {:webserver {:port 10000}}
      (let [client (async/create-client {})]
        (testing "Set a cookie using Clojure API"
          (let [response (common/get client "http://localhost:10000/cookietest")]
            (is (= 200 (:status @response)))))
        (testing "Check if cookie still exists"
          (let [response (common/get client "http://localhost:10000/cookiecheck")]
            (is (= 200 (:status @response)))))))))

(deftest request-with-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (let [client (HttpAsyncClients/createDefault)
            opts   {:method :get :url "http://localhost:10000/hello/"}]
        (.start client)
        (testing "GET request works with request-with-client"
          (let [response (async/request-with-client opts nil client)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (testing "Client persists when passed to request-with-client"
          (let [response (async/request-with-client opts nil client)]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))
        (.close client)))))

(deftest query-params-test-async
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-params-web-service]
      {:webserver {:port 8080}}
        (testing "URL Query Parameters work with the Java client"
          (let [client (Async/createClient (ClientOptions.))]
            (try
              (let [request-options (RequestOptions.
                                      (URI. "http://localhost:8080/params?foo=bar&baz=lux"))
                    response        (.get client request-options)]
                (is (= 200 (.getStatus (.deref response))))
                (is (= queryparams (read-string (slurp (.getBody
                                                         (.deref response)))))))
              (finally
                (.close client)))))
        (testing "URL Query Parameters work with the clojure client"
          (with-open [client (async/create-client {})]
            (let [opts     {:method       :get
                            :url          "http://localhost:8080/params/"
                            :query-params queryparams
                            :as           :text}
                  response (common/get client "http://localhost:8080/params" opts)]
                (is (= 200 (:status @response)))
                (is (= queryparams (read-string (:body @response)))))))
        (testing "URL Query Parameters can be set directly in the URL"
          (with-open [client (async/create-client {})]
            (let [response (common/get client
                                       "http://localhost:8080/params?paramone=one"
                                       {:as :text})]
              (is (= 200 (:status @response)))
              (is (= (str {"paramone" "one"}) (:body @response))))))
        (testing (str "URL Query Parameters set in URL are overwritten if params "
                      "are also specified in options map")
          (with-open [client (async/create-client {})]
            (let [response (common/get client
                                       "http://localhost:8080/params?paramone=one&foo=lux"
                                       query-options)]
              (is (= 200 (:status @response)))
              (is (= queryparams (read-string (:body @response))))))))))

(deftest redirect-test-async
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service redirect-web-service]
      {:webserver {:port 8080}}
      (testing (str "redirects on POST not followed by persistent Java client "
                    "when forceRedirects option not set to true")
        (let [client (Async/createClient (ClientOptions.))]
          (try
            (let [request-options  (RequestOptions.
                                     (URI. "http://localhost:8080/hello"))
                  response         (.post client request-options)]
              (is (= 302 (.getStatus (.deref response)))))
            (finally
              (.close client)))))
      (testing "redirects on POST followed by Java client when option is set"
        (let [client (Async/createClient (.. (ClientOptions.)
                                             (setForceRedirects true)))]
          (try
            (let [request-options (RequestOptions.
                                    (URI. "http://localhost:8080/hello"))
                  response        (.post client request-options)]
              (is (= 200 (.getStatus (.deref response))))
              (is (= "Hello, World!" (slurp (.getBody (.deref response))))))
            (finally
              (.close client)))))
      (testing "redirects not followed by Java client when :follow-redirects is false"
        (let [client (Async/createClient (.. (ClientOptions.)
                                             (setFollowRedirects false)))]
          (try
            (let [request-options (RequestOptions.
                                    (URI. "http://localhost:8080/hello"))
                  response        (.get client request-options)]
              (is (= 302 (.getStatus (.deref response)))))
            (finally
              (.close client)))))
      (testing ":follow-redirects overrides :force-redirects for Java client"
        (let [client (Async/createClient (.. (ClientOptions.)
                                             (setFollowRedirects false)
                                             (setForceRedirects true)))]
          (try
            (let [request-options (RequestOptions.
                                    (URI. "http://localhost:8080/hello"))
                  response        (.get client request-options)]
              (is (= 302 (.getStatus (.deref response)))))
            (finally
              (.close client)))))
      (testing (str "redirects on POST not followed by clojure client "
                    "when :force-redirects is not set to true")
        (with-open [client (async/create-client {:force-redirects false})]
          (let [opts     {:method :post
                          :url    "http://localhost:8080/hello"
                          :as     :text}
                response (common/post client "http://localhost:8080/hello" opts)]
            (is (= 302 (:status @response))))))
      (testing (str "redirects on POST followed by persistent clojure client "
                    "when option is set")
        (with-open [client (async/create-client {:force-redirects true})]
          (let [response (common/post client
                                      "http://localhost:8080/hello"
                                      {:as :text})]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (:body @response))))))
      (testing (str "persistent clojure client does not follow redirects when "
                    ":follow-redirects is set to false")
        (with-open [client (async/create-client {:follow-redirects false})]
          (let [response (common/get client
                                     "http://localhost:8080/hello"
                                     {:as :text})]
            (is (= 302 (:status @response))))))
      (testing ":follow-redirects overrides :force-redirects with persistent clj client"
        (with-open [client (async/create-client {:follow-redirects false
                                                 :force-redirects true})]
          (let [response (common/get client
                                     "http://localhost:8080/hello"
                                     {:as :text})]
            (is (= 302 (:status @response)))))))))

(deftest short-connect-timeout-persistent-java-test-async
  (testing (str "connection times out properly for java persistent client "
                "async request with short timeout")
    (with-open [client (-> (ClientOptions.)
                           (.setConnectTimeoutMilliseconds 250)
                           (Async/createClient))]
      (let [request-options     (RequestOptions. "http://127.0.0.255:65535")
            time-before-connect (System/currentTimeMillis)]
        (is (connect-exception-thrown? (-> client
                                           (.get request-options)
                                           (.deref)
                                           (.getError)))
            "Unexpected result for connection attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Connection attempt took significantly longer than timeout")))))

(deftest short-connect-timeout-persistent-clojure-test-async
  (testing (str "connection times out properly for clojure persistent client "
                "async request with short timeout")
    (with-open [client (async/create-client
                         {:connect-timeout-milliseconds 250})]
      (let [time-before-connect (System/currentTimeMillis)]
        (is (connect-exception-thrown? (-> @(common/get
                                              client
                                              "http://127.0.0.255:65535")
                                           :error))
            "Unexpected result for connection attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Connection attempt took significantly longer than timeout")))))

(deftest longer-connect-timeout-test-async
  (testing "connection succeeds for async request with longer connect timeout"
    (testlogging/with-test-logging
      (testwebserver/with-test-webserver app port
        (let [url (str "http://localhost:" port "/hello")]
          (testing "java persistent async client"
            (with-open [client (-> (ClientOptions.)
                                   (.setConnectTimeoutMilliseconds 2000)
                                   (Async/createClient))]
              (let [response (-> client
                                 (.get (RequestOptions. url))
                                 (.deref))]
                (is (= 200 (.getStatus response)))
                (is (= "Hello, World!" (slurp (.getBody response)))))))
          (testing "clojure persistent async client"
            (with-open [client (async/create-client
                                 {:connect-timeout-milliseconds 2000})]
              (let [response @(common/get client url {:as :text})]
                (is (= 200 (:status response)))
                (is (= "Hello, World!" (:body response)))))))))))

(deftest short-socket-timeout-persistent-java-test-async
  (testing (str "socket read times out properly for persistent java async "
                "request with short timeout")
    (with-open [client (-> (ClientOptions.)
                           (.setSocketTimeoutMilliseconds 1)
                           (Async/createClient))
                server (ServerSocket. 0)]
      (let [request-options     (-> "http://127.0.0.1:"
                                    (str (.getLocalPort server))
                                    (RequestOptions.))
            time-before-connect (System/currentTimeMillis)]
        (is (instance? SocketTimeoutException (-> client
                                                  (.get request-options)
                                                  (.deref)
                                                  (.getError)))
            "Unexpected result for get attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Get attempt took significantly longer than timeout")))))

(deftest short-socket-timeout-persistent-clojure-test-async
  (testing (str "socket read times out properly for clojure persistent client "
                "async request with short timeout")
    (with-open [client (async/create-client
                         {:socket-timeout-milliseconds 250})
                server (ServerSocket. 0)]
      (let [url                 (str "http://127.0.0.1:" (.getLocalPort server))
            time-before-connect (System/currentTimeMillis)]
        (is (instance? SocketTimeoutException
                       (-> @(common/get client url)
                           :error))
            "Unexpected result for get attempt")
        (is (elapsed-within-range? time-before-connect 2000)
            "Get attempt took significantly longer than timeout")))))

(deftest longer-socket-timeout-test-async
  (testing "get succeeds for async request with longer socket timeout"
    (testlogging/with-test-logging
      (testwebserver/with-test-webserver app port
        (let [url (str "http://localhost:" port "/hello")]
          (testing "java persistent async client"
            (with-open [client (-> (ClientOptions.)
                                   (.setSocketTimeoutMilliseconds 2000)
                                   (Async/createClient))]
              (let [response (-> client
                                 (.get (RequestOptions. url))
                                 (.deref))]
                (is (= 200 (.getStatus response)))
                (is (= "Hello, World!" (slurp (.getBody response)))))))
          (testing "clojure persistent async client"
            (with-open [client (async/create-client
                                 {:socket-timeout-milliseconds 2000})]
              (let [response @(common/get client url {:as :text})]
                (is (= 200 (:status response)))
                (is (= "Hello, World!" (:body response)))))))))))

(deftest empty-content-type-async
  (testing "content-type parsing handles invalid content-type"
    (testlogging/with-test-logging
     ;; while the content-type is empty, the request actually gets sent
     ;; as ";<character-set>", which is invalid
      (testwebserver/with-test-webserver app-with-empty-content-type port
         (let [url (str "http://localhost:" port "/hello")]
           (testing "java persistent async client"
             (with-open [client (-> (ClientOptions.)
                                    (Async/createClient))]
               (let [response (-> client
                                  (.get (RequestOptions. url))
                                  (.deref))]
                 (is (= 200 (.getStatus response))))))
           (testing "clojure persistent async client"
             (with-open [client (async/create-client {})]
               (let [response @(common/get client url {:as :text})]
                 (is (= 200 (:status response)))))))))))

(deftest accept-language-async
  (testing "client passes on the user-locale in Accept-Language header"
    (testlogging/with-test-logging
      (testwebserver/with-test-webserver app-with-language-header-echo port
        (i18n/with-user-locale (Locale. "es" "ES")
          (let [url (str "http://localhost:" port "/hello")]
            (testing "clojure persistent async client"
              (with-open [client (async/create-client {})]
                (let [response @(common/get client url {:as :text})]
                  (is (= 200 (:status response)))
                  (is (= "es-ES" (:body response))))))))))))

(deftest client-route-and-total-limits
  (testing "client limits 2 requests per route by default"
    (let [actual-count (atom 0)
          countdown (CountDownLatch. 3)
          fake-app (fn [_]
                     (swap! actual-count inc)
                     (.countDown countdown)
                     (.await countdown)
                     {:status 200
                      :body "Hello, World!"})]
      (testlogging/with-test-logging
        (testwebserver/with-test-webserver fake-app port
          (let [url (str "http://localhost:" port "/hello")]
            (testing "clojure persistent async client"
              (with-open [client (async/create-client {})]
                (dotimes [n 10] (future (common/get client url {:as :text})))
                (is (= false (.await countdown 5 TimeUnit/SECONDS)))
                (is (= 2 @actual-count)))))))))

  (testing "passing client route limit of 0 selects default behavior (a limit of 2)"
    (let [actual-count (atom 0)
          countdown (CountDownLatch. 3)
          fake-app (fn [_]
                     (swap! actual-count inc)
                     (.countDown countdown)
                     (.await countdown)
                     {:status 200
                      :body "Hello, World!"})]
      (testlogging/with-test-logging
       (testwebserver/with-test-webserver fake-app port
         (let [url (str "http://localhost:" port "/hello")]
           (testing "clojure persistent async client"
             (with-open [client (async/create-client {:max-connections-per-route 0})]
               (dotimes [n 10] (future (common/get client url {:as :text})))
               (is (= false (.await countdown 5 TimeUnit/SECONDS)))
               (is (= 2 @actual-count)))))))))


  (testing "client limits specified requests per route"
    (let [actual-count (atom 0)
          countdown (CountDownLatch. 4)
          fake-app (fn [_]
                     (swap! actual-count inc)
                     (.countDown countdown)
                     (.await countdown)
                     {:status 200
                      :body "Hello, World!"})]
      (testlogging/with-test-logging
       (testwebserver/with-test-webserver fake-app port
         (let [url (str "http://localhost:" port "/hello")]
           (testing "clojure persistent async client"
             (with-open [client (async/create-client {:max-connections-per-route 3})]
               (dotimes [n 10] (future (common/get client url {:as :text})))
               (is (= false (.await countdown 5 TimeUnit/SECONDS)))
               (is (= 3 @actual-count)))))))))

  (testing "client route limit of 11 does not limit requests per route when less than 11"
    (let [actual-count (atom 0)
          countdown (CountDownLatch. 10)
          fake-app (fn [_]
                     (swap! actual-count inc)
                     (.countDown countdown)
                     (.await countdown)
                     {:status 200
                      :body "Hello, World!"})]
      (testlogging/with-test-logging
       (testwebserver/with-test-webserver fake-app port
         (let [url (str "http://localhost:" port "/hello")]
           (testing "clojure persistent async client"
             (with-open [client (async/create-client {:max-connections-per-route 11})]
               (dotimes [n 10] (future (common/get client url {:as :text})))
               (is (= true (.await countdown 5 TimeUnit/SECONDS)))
               (is (= 10 @actual-count)))))))))

  (testing "overall limit applies"
    (let [actual-count (atom 0)
          countdown (CountDownLatch. 4)
          fake-app (fn [_]
                     (swap! actual-count inc)
                     (.countDown countdown)
                     (.await countdown)
                     {:status 200
                      :body "Hello, World!"})]
      (testlogging/with-test-logging
       (testwebserver/with-test-webserver fake-app port
         (let [url (str "http://localhost:" port "/hello")]
           (testing "clojure persistent async client"
             (with-open [client (async/create-client {:max-connections-per-route 11
                                                      :max-connections-total 3})]
               (dotimes [n 10] (future (common/get client url {:as :text})))
               (is (= false (.await countdown 5 TimeUnit/SECONDS)))
               (is (= 3 @actual-count))))))))))
