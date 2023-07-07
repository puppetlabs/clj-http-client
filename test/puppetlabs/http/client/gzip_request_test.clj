(ns puppetlabs.http.client.gzip-request-test
  (:import (com.puppetlabs.http.client Sync
                                       SimpleRequestOptions
                                       ResponseBodyType
                                       CompressType)
           (java.io ByteArrayInputStream FilterInputStream)
           (java.net URI)
           (java.util.zip GZIPInputStream))
  (:require [clojure.test :refer :all]
            [cheshire.core :as cheshire]
            [schema.test :as schema-test]
            [puppetlabs.http.client.sync :as http-client]
            [puppetlabs.http.client.test-common :refer [connect-exception-thrown?]]
            [puppetlabs.trapperkeeper.testutils.webserver :as testwebserver]))

(use-fixtures :once schema-test/validate-schemas)

(defn req-body-app
  [req]
  (let [response {:request-content-encoding (get-in req [:headers "content-encoding"])
                  :request-body-decompressed (slurp
                                              (GZIPInputStream. (:body req))
                                              :encoding "utf-8")}]
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (cheshire/generate-string response)}))

(def short-request-body "gzip me�")

(def big-request-body
  (apply str (repeat 4000 "and�i�said�hey�yeah�yeah�whats�going�on")))

(defn string->byte-array-input-stream
  [source is-closed-atom]
  (let [bis (-> source
                (.getBytes)
                (ByteArrayInputStream.))]
    (proxy [FilterInputStream] [bis]
      (close []
        (reset! is-closed-atom true)
        (proxy-super close)))))

(defn post-gzip-clj-request
  [port body]
  (-> (http-client/post (format "http://localhost:%d" port)
                        {:body body
                         :headers {"Content-Type" "text/plain; charset=utf-8"}
                         :compress-request-body :gzip
                         :as :text})
      :body
      (cheshire/parse-string true)))

(defn post-gzip-java-request
  [port body]
  (-> (SimpleRequestOptions. (URI. (format "http://localhost:%d/hello/" port)))
      (.setBody body)
      (.setHeaders {"Content-Type" "text/plain; charset=utf-8"})
      (.setRequestBodyCompression CompressType/GZIP)
      (.setAs ResponseBodyType/TEXT)
      (Sync/post)
      (.getBody)
      (cheshire/parse-string true)))

(deftest clj-sync-client-gzip-requests
  (testing "for clojure sync client"
    (testwebserver/with-test-webserver
     req-body-app
     port
     (testing "short string body is gzipped in request"
       (let [response (post-gzip-clj-request port short-request-body)]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= short-request-body (:request-body-decompressed response)))))
     (testing "big string body is gzipped in request"
       (let [response (post-gzip-clj-request port big-request-body)]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= big-request-body (:request-body-decompressed response)))))
     (testing "short inputstream body is gzipped in request"
       (let [is-closed (atom false)
             response (post-gzip-clj-request
                       port
                       (string->byte-array-input-stream short-request-body
                                                        is-closed))]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= short-request-body (:request-body-decompressed response)))
         (is @is-closed "input stream was not closed after request")))
     (testing "big inputstream body is gzipped in request"
       (let [is-closed (atom false)
             response (post-gzip-clj-request
                       port
                       (string->byte-array-input-stream big-request-body
                                                        is-closed))]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= big-request-body (:request-body-decompressed response)))
         (is @is-closed "input stream was not closed after request"))))))

(deftest java-sync-client-gzip-requests
  (testing "for java sync client"
    (testwebserver/with-test-webserver
     req-body-app
     port
     (testing "short string body is gzipped in request"
       (let [response (post-gzip-java-request port short-request-body)]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= short-request-body (:request-body-decompressed response)))))
     (testing "big string body is gzipped in request"
       (let [response (post-gzip-java-request port big-request-body)]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= big-request-body (:request-body-decompressed response)))))
     (testing "short inputstream body is gzipped in request"
       (let [is-closed (atom false)
             response (post-gzip-java-request
                       port
                       (string->byte-array-input-stream short-request-body
                                                        is-closed))]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= short-request-body (:request-body-decompressed response)))
         (is @is-closed "input stream was not closed after request")))
     (testing "big inputstream body is gzipped in request"
       (let [is-closed (atom false)
             response (post-gzip-java-request
                       port
                       (string->byte-array-input-stream big-request-body
                                                        is-closed))]
         (is (= "gzip" (:request-content-encoding response)))
         (is (= big-request-body (:request-body-decompressed response)))
         (is @is-closed "input stream was not closed after request"))))))

(deftest connect-exception-during-gzip-request-returns-failure
  (testing "connection exception during gzip request returns failure"
    (let [is-closed (atom false)]
      (is (connect-exception-thrown?
           (http-client/post "http://localhost:65535"
                             {:body (string->byte-array-input-stream
                                     short-request-body
                                     is-closed)
                              :compress-request-body :gzip
                              :as :text})))
      (is @is-closed "input stream was not closed after request"))))
