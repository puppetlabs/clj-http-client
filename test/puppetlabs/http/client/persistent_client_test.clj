(ns puppetlabs.http.client.persistent-client-test
  (:import (com.puppetlabs.http.client AsyncHttpClient RequestOptions))
  (:require [clojure.test :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.persistent-async :as p-async]
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

(deftest persistent-async-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
       {:webserver {:port 10000}}
       (let [client (p-async/create-client {})]
         (testing "HEAD request with persistent async client"
                  (let [response (p-async/head client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= nil (:body @response)))))
         (testing "GET request with persistent async client"
                  (let [response (p-async/get client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= "Hello, World!" (slurp (:body @response))))))
         (testing "POST request with persistent async client"
                  (let [response (p-async/post client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= "Hello, World!" (slurp (:body @response))))))
         (testing "PUT request with persistent async client"
                  (let [response (p-async/put client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= "Hello, World!" (slurp (:body @response))))))
         (testing "DELETE request with persistent async client"
                  (let [response (p-async/delete client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= "Hello, World!" (slurp (:body @response))))))
         (testing "TRACE request with persistent async client"
                  (let [response (p-async/trace client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= "Hello, World!" (slurp (:body @response))))))
         (testing "OPTIONS request with persistent async client"
                  (let [response (p-async/options client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= "Hello, World!" (slurp (:body @response))))))
         (testing "PATCH request with persistent async client"
                  (let [response (p-async/patch client "http://localhost:10000/hello/")]
                    (is (= 200 (:status @response)))
                    (is (= "Hello, World!" (slurp (:body @response))))))
         (.close client)))))
