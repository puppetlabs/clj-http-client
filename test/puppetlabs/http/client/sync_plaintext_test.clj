(ns puppetlabs.http.client.sync-plaintext-test
  (:import (com.puppetlabs.http.client SyncHttpClient RequestOptions
                                       HttpClientException ResponseBodyType)
           (javax.net.ssl SSLHandshakeException)
           (java.io ByteArrayInputStream InputStream)
           (java.nio.charset Charset)
           (org.apache.http.impl.nio.client HttpAsyncClients)
           (java.net URI))
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.test-common :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.sync :as sync]
            [puppetlabs.http.client.common :as common]
            [schema.test :as schema-test]
            [clojure.java.io :as io]))

(use-fixtures :once schema-test/validate-schemas)

(defn app
  [req]
  {:status 200
   :body "Hello, World!"})

(tk/defservice test-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler app "/hello")
        context))

(defn basic-test
  [http-method java-method clj-fn]
  (testing (format "sync client: HTTP method: '%s'" http-method)
    (testlogging/with-test-logging
      (testutils/with-app-with-config app
        [jetty9/jetty9-service test-web-service]
        {:webserver {:port 10000}}
        (testing "java sync client"
          (let [options (RequestOptions. (URI. "http://localhost:10000/hello/"))
                response (java-method options)]
            (is (= 200 (.getStatus response)))
            (is (= "Hello, World!" (slurp (.getBody response))))))
        (testing "clojure sync client"
          (let [response (clj-fn "http://localhost:10000/hello/")]
            (is (= 200 (:status response)))
            (is (= "Hello, World!" (slurp (:body response))))))))))

(deftest sync-client-head-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client"
        (let [options (RequestOptions. (URI. "http://localhost:10000/hello/"))
              response (SyncHttpClient/head options)]
          (is (= 200 (.getStatus response)))
          (is (= nil (.getBody response)))))
      (testing "clojure sync client"
        (let [response (sync/head "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= nil (:body response))))))))

(deftest sync-client-get-test
  (basic-test "GET" #(SyncHttpClient/get %) sync/get))

(deftest sync-client-post-test
  (basic-test "POST" #(SyncHttpClient/post %) sync/post))

(deftest sync-client-put-test
  (basic-test "PUT" #(SyncHttpClient/put %) sync/put))

(deftest sync-client-delete-test
  (basic-test "DELETE" #(SyncHttpClient/delete %) sync/delete))

(deftest sync-client-trace-test
  (basic-test "TRACE" #(SyncHttpClient/trace %) sync/trace))

(deftest sync-client-options-test
  (basic-test "OPTIONS" #(SyncHttpClient/options %) sync/options))

(deftest sync-client-patch-test
  (basic-test "PATCH" #(SyncHttpClient/patch %) sync/patch))

(deftest sync-client-persistent-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
    [jetty9/jetty9-service test-web-service]
    {:webserver {:port 10000}}
    (let [client (sync/create-client {})]
      (testing "HEAD request with persistent sync client"
        (let [response (common/head client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= nil (:body response)))))
      (testing "GET request with persistent sync client"
        (let [response (common/get client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "POST request with persistent sync client"
        (let [response (common/post client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "PUT request with persistent sync client"
        (let [response (common/put client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "DELETE request with persistent sync client"
        (let [response (common/delete client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "TRACE request with persistent sync client"
        (let [response (common/trace client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "OPTIONS request with persistent sync client"
        (let [response (common/options client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "PATCH request with persistent sync client"
        (let [response (common/patch client "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (common/close client)
      (is (thrown? IllegalStateException
                   (common/get client "http://localhost:10000/hello")))))))

(deftest sync-client-as-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client: :as unspecified"
        (let [options (RequestOptions. (URI. "http://localhost:10000/hello/"))
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (instance? InputStream (.getBody response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "java sync client: :as :stream"
        (let [options (.. (RequestOptions. (URI. "http://localhost:10000/hello/"))
                          (setAs ResponseBodyType/STREAM))
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (instance? InputStream (.getBody response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "java sync client: :as :text"
        (let [options (.. (RequestOptions. (URI. "http://localhost:10000/hello/"))
                          (setAs ResponseBodyType/TEXT))
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (string? (.getBody response)))
          (is (= "Hello, World!" (.getBody response)))))
      (testing "clojure sync client: :as unspecified"
        (let [response (sync/get "http://localhost:10000/hello/")]
          (is (= 200 (:status response)))
          (is (instance? InputStream (:body response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "clojure sync client: :as :stream"
        (let [response (sync/get "http://localhost:10000/hello/" {:as :stream})]
          (is (= 200 (:status response)))
          (is (instance? InputStream (:body response)))
          (is (= "Hello, World!" (slurp (:body response))))))
      (testing "clojure sync client: :as :text"
        (let [response (sync/get "http://localhost:10000/hello/" {:as :text})]
          (is (= 200 (:status response)))
          (is (string? (:body response)))
          (is (= "Hello, World!" (:body response))))))))

(deftest request-with-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (let [client (HttpAsyncClients/createDefault)
            opts   {:method :get :url "http://localhost:10000/hello/"}]
        (.start client)
        (testing "GET request works with request-with-client"
          (let [response (sync/request-with-client opts client)]
            (is (= 200 (:status response)))
            (is (= "Hello, World!" (slurp (:body response))))))
        (testing "Client persists when passed to request-with-client"
          (let [response (sync/request-with-client opts client)]
            (is (= 200 (:status response)))
            (is (= "Hello, World!" (slurp (:body response))))))
        (.close client)))))

(defn header-app
  [req]
  (let [val (get-in req [:headers "fooheader"])]
    {:status  200
     :headers {"myrespheader" val}
     :body    val}))

(tk/defservice test-header-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
       (add-ring-handler header-app "/hello")
       context))

(deftest sync-client-request-headers-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config header-app
      [jetty9/jetty9-service test-header-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client"
        (let [options (-> (RequestOptions. (URI. "http://localhost:10000/hello/"))
                          (.setHeaders {"fooheader" "foo"}))
              response (SyncHttpClient/post options)]
          (is (= 200 (.getStatus response)))
          (is (= "foo" (slurp (.getBody response))))
          (is (= "foo" (-> (.getHeaders response) (.get "myrespheader"))))))
      (testing "clojure sync client"
        (let [response (sync/post "http://localhost:10000/hello/" {:headers {"fooheader" "foo"}})]
          (is (= 200 (:status response)))
          (is (= "foo" (slurp (:body response))))
          (is (= "foo" (get-in response [:headers "myrespheader"]))))))))

(defn req-body-app
  [req]
  (let [encoding (if (:character-encoding req)
                   (:character-encoding req)
                   "UTF-8")]
    {:status  200
     :headers {"Content-Type"(str "text/plain; charset=" encoding)}
     :body    (slurp (:body req))}))

(tk/defservice test-body-web-service
               [[:WebserverService add-ring-handler]]
               (init [this context]
                     (add-ring-handler req-body-app "/hello")
                     context))

(deftest sync-client-request-body-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config req-body-app
      [jetty9/jetty9-service test-body-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client: string body for post request"
        (let [options (-> (RequestOptions. (URI. "http://localhost:10000/hello/"))
                          (.setBody "foo�"))
              response (SyncHttpClient/post options)]
          (is (= 200 (.getStatus response)))
          (is (= "foo�" (slurp (.getBody response))))))
      (testing "java sync client: input stream body for post request"
        (let [options (-> (RequestOptions. (URI. "http://localhost:10000/hello/"))
                          (.setBody (ByteArrayInputStream.
                                      (.getBytes "foo�" "UTF-8"))))
              response (SyncHttpClient/post options)]
          (is (= 200 (.getStatus response)))
          (is (= "foo�" (slurp (.getBody response))))))
      (testing "clojure sync client: string body for post request"
        (let [response (sync/post "http://localhost:10000/hello/"
                                  {:body "foo�"})]
          (is (= 200 (:status response)))
          (is (= "foo�" (slurp (:body response))))))
      (testing "clojure sync client: input stream body for post request"
        (let [response (sync/post "http://localhost:10000/hello/"
                                  {:body (io/input-stream
                                           (.getBytes "foo�" "UTF-8"))})]
          (is (= 200 (:status response)))
          (is (= "foo�" (slurp (:body response)))))))))

(def compressible-body (apply str (repeat 1000 "f")))

(defn compression-app
  [req]
  {:status  200
   :headers {"orig-accept-encoding" (get-in req [:headers "accept-encoding"])
             "content-type" "text/plain"
             "charset" "UTF-8"}
   :body    compressible-body})

(tk/defservice test-compression-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
       (add-ring-handler compression-app "/hello")
       context))

(defn test-compression
  [desc opts accept-encoding content-encoding content-should-match?]
  (testlogging/with-test-logging
    (testutils/with-app-with-config req-body-app
      [jetty9/jetty9-service test-compression-web-service]
      {:webserver {:port 10000}}
      (testing (str "java sync client: compression headers / response: " desc)
        (let [java-opts (cond-> (RequestOptions. (URI. "http://localhost:10000/hello/"))
                                (contains? opts :decompress-body) (.setDecompressBody (:decompress-body opts))
                                (contains? opts :headers) (.setHeaders (:headers opts)))
              response (SyncHttpClient/get java-opts)]
          (is (= 200 (.getStatus response)))
          (is (= accept-encoding (.. response getHeaders (get "orig-accept-encoding"))))
          (is (= content-encoding (.. response getOrigContentEncoding)))
          (if content-should-match?
            (is (= compressible-body (slurp (.getBody response))))
            (is (not= compressible-body (slurp (.getBody response)))))))
      (testing (str "clojure sync client: compression headers / response: " desc)
        (let [response (sync/post "http://localhost:10000/hello/" opts)]
          (is (= 200 (:status response)))
          (is (= accept-encoding (get-in response [:headers "orig-accept-encoding"])))
          (is (= content-encoding (:orig-content-encoding response)))
          (if content-should-match?
            (is (= compressible-body (slurp (:body response))))
            (is (not= compressible-body (slurp (:body response))))))))))

(deftest sync-client-compression-test
  (test-compression "default" {} "gzip, deflate" "gzip" true))

(deftest sync-client-compression-gzip-test
  (test-compression "explicit gzip" {:headers {"accept-encoding" "gzip"}} "gzip" "gzip" true))

(deftest sync-client-compression-disabled-test
  (test-compression "explicit disable" {:decompress-body false} nil nil true))

(deftest sync-client-decompression-disabled-test
  (test-compression "explicit disable" {:headers {"accept-encoding" "gzip"}
                                        :decompress-body false} "gzip" "gzip" false))

(deftest query-params-test-sync
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-params-web-service]
      {:webserver {:port 8080}}
      (testing "URL Query Parameters work with the Java client"
        (let [options (RequestOptions. (URI. "http://localhost:8080/params?foo=bar&baz=lux"))]
          (let [response (SyncHttpClient/get options)]
            (is (= 200 (.getStatus response)))
            (is (= queryparams (read-string (slurp (.getBody response))))))))

      (testing "URL Query Parameters work with the clojure client"
        (let [opts {:method       :get
                    :url          "http://localhost:8080/params/"
                    :query-params queryparams
                    :as           :text}
              response (sync/get "http://localhost:8080/params" opts)]
          (is (= 200 (:status response)))
          (is (= queryparams (read-string (:body response)))))))))

(deftest redirect-test-sync
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service redirect-web-service]
      {:webserver {:port 8080}}
      (testing (str "redirects on POST not followed by Java client "
                    "when forceRedirects option not set to true")
        (let [options  (RequestOptions. (URI. "http://localhost:8080/hello"))
              response (SyncHttpClient/post options)]
          (is (= 302 (.getStatus response)))))
      (testing "redirects on POST followed by Java client when option is set"
        (let [options (.. (RequestOptions. (URI. "http://localhost:8080/hello"))
                          (setForceRedirects true))
              response (SyncHttpClient/post options)]
          (is (= 200 (.getStatus response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "redirects not followed by Java client when :follow-redirects is false"
        (let [options (.. (RequestOptions. (URI. "http://localhost:8080/hello"))
                          (setFollowRedirects false))
              response (SyncHttpClient/get options)]
          (is (= 302 (.getStatus response)))))
      (testing ":follow-redirects overrides :force-redirects for Java client"
        (let [options (.. (RequestOptions. (URI. "http://localhost:8080/hello"))
                          (setFollowRedirects false)
                          (setForceRedirects true))
              response (SyncHttpClient/get options)]
          (is (= 302 (.getStatus response)))))
      (testing (str "redirects on POST not followed by clojure client "
                    "when :force-redirects is not set to true")
        (let [opts     {:method           :post
                        :url              "http://localhost:8080/hello"
                        :as               :text
                        :force-redirects  false}
              response (sync/post "http://localhost:8080/hello" opts)]
          (is (= 302 (:status response)))))
      (testing "redirects on POST followed by clojure client when option is set"
        (let [opts     {:method           :post
                        :url              "http://localhost:8080/hello"
                        :as               :text
                        :force-redirects  true}
              response (sync/post "http://localhost:8080/hello" opts)]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (:body response)))))
      (testing (str "redirects not followed by clojure client when :follow-redirects "
                    "is set to false")
        (let [response (sync/get "http://localhost:8080/hello" {:as :text
                                                                 :follow-redirects false})]
          (is (= 302 (:status response)))))
      (testing ":follow-redirects overrides :force-redirects with clojure client"
        (let [response (sync/get "http://localhost:8080/hello" {:as :text
                                                                 :follow-redirects false
                                                                 :force-redirects true})]
          (is (= 302 (:status response)))))
      (testing (str "redirects on POST followed by persistent clojure client "
                    "when option is set")
        (let [client (sync/create-client {:force-redirects true})
              response (common/post client "http://localhost:8080/hello" {:as :text})]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (:body response)))
          (common/close client)))
      (testing (str "persistent clojure client does not follow redirects when "
                    ":follow-redirects is set to false")
        (let [client (sync/create-client {:follow-redirects false})
              response (common/get client "http://localhost:8080/hello" {:as :text})]
          (is (= 302 (:status response)))
          (common/close client)))
      (testing ":follow-redirects overrides :force-redirects with persistent clj client"
        (let [client (sync/create-client {:follow-redirects false
                                           :force-redirects true})
              response (common/get client "http://localhost:8080/hello" {:as :text})]
          (is (= 302 (:status response)))
          (common/close client))))))
