(ns puppetlabs.http.client.async-plaintext-test
  (:import (com.puppetlabs.http.client AsyncHttpClient RequestOptions))
  (:require [clojure.test :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.async :as async]
            [schema.test :as schema-test]))

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
  (testing (format "async client: HTTP method: '%s'" http-method)
    (testlogging/with-test-logging
      (testutils/with-app-with-config app
        [jetty9/jetty9-service test-web-service]
        {:webserver {:port 10000}}
        (testing "java async client"
          (let [options (RequestOptions. "http://localhost:10000/hello/")
                response (java-method options)]
            (is (= 200 (.getStatus (.deref response))))
            (is (= "Hello, World!" (slurp (.getBody (.deref response)))))))
        (testing "clojure async client"
          (let [response (clj-fn "http://localhost:10000/hello/")]
            (is (= 200 (:status @response)))
            (is (= "Hello, World!" (slurp (:body @response))))))))))

(deftest async-client-head-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:port 10000}}
      (testing "java sync client"
        (let [options (RequestOptions. "http://localhost:10000/hello/")
              response (AsyncHttpClient/head options)]
          (is (= 200 (.getStatus (.deref response))))
          (is (= nil (.getBody (.deref response))))))
      (testing "clojure sync client"
        (let [response (async/head "http://localhost:10000/hello/")]
          (is (= 200 (:status @response)))
          (is (= nil (:body @response))))))))

(deftest async-client-get-test
  (basic-test "GET" #(AsyncHttpClient/get %) async/get))

(deftest async-client-post-test
  (basic-test "POST" #(AsyncHttpClient/post %) async/post))

(deftest async-client-put-test
  (basic-test "PUT" #(AsyncHttpClient/put %) async/put))

(deftest async-client-delete-test
  (basic-test "DELETE" #(AsyncHttpClient/delete %) async/delete))

(deftest async-client-trace-test
  (basic-test "TRACE" #(AsyncHttpClient/trace %) async/trace))

(deftest async-client-options-test
  (basic-test "OPTIONS" #(AsyncHttpClient/options %) async/options))

(deftest async-client-patch-test
  (basic-test "PATCH" #(AsyncHttpClient/patch %) async/patch))