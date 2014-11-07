(ns puppetlabs.http.client.async-plaintext-test
  (:import (com.puppetlabs.http.client RequestOptions)
           (org.apache.http.impl.nio.client HttpAsyncClients)
           (java.net URI))
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.test-common :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.common :as common]
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

(deftest persistent-async-client-test
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
    [jetty9/jetty9-service test-web-service]
    {:webserver {:port 10000}}
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
      (testing "client closes properly"
        (common/close client)
        (is (thrown? IllegalStateException (common/get client "http://localhost:10000/hello/"))))))))

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

(deftest redirect-test-async
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service redirect-web-service]
      {:webserver {:port 8080}}
      (testing (str "redirects on POST followed by persistent clojure client "
                    "when option is set")
        (let [client (async/create-client {:force-redirects true})
              response (common/post client "http://localhost:8080/hello" {:as :text})]
          (is (= 200 (:status @response)))
          (is (= "Hello, World!" (:body @response)))
          (common/close client)))
      (testing (str "persistent clojure client does not follow redirects when "
                    ":follow-redirects is set to false")
        (let [client (async/create-client {:follow-redirects false})
              response (common/get client "http://localhost:8080/hello" {:as :text})]
          (is (= 302 (:status @response)))
          (common/close client)))
      (testing ":follow-redirects overrides :force-redirects with persistent clj client"
        (let [client (async/create-client {:follow-redirects false
                                           :force-redirects true})
              response (common/get client "http://localhost:8080/hello" {:as :text})]
          (is (= 302 (:status @response)))
          (common/close client))))))
