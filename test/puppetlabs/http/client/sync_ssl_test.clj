(ns puppetlabs.http.client.sync-ssl-test
  (:import (com.puppetlabs.http.client SyncHttpClient RequestOptions
                                       HttpClientException)
           (javax.net.ssl SSLHandshakeException))
  (:require [clojure.test :refer :all]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.http.client.sync :as sync]
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

(deftest sync-client-test-from-pems
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:ssl-host    "0.0.0.0"
                   :ssl-port    10080
                   :ssl-ca-cert "./dev-resources/ssl/ca.pem"
                   :ssl-cert    "./dev-resources/ssl/cert.pem"
                   :ssl-key     "./dev-resources/ssl/key.pem"}}
      (testing "java sync client"
        (let [options (.. (RequestOptions. "https://localhost:10080/hello/")
                          (setSslCert "./dev-resources/ssl/cert.pem")
                          (setSslKey "./dev-resources/ssl/key.pem")
                          (setSslCaCert "./dev-resources/ssl/ca.pem"))
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "clojure sync client"
        (let [response (sync/get "https://localhost:10080/hello/"
                                 {:ssl-cert "./dev-resources/ssl/cert.pem"
                                  :ssl-key "./dev-resources/ssl/key.pem"
                                  :ssl-ca-cert "./dev-resources/ssl/ca.pem"})]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response)))))))))

(deftest sync-client-test-from-ca-cert
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:ssl-host    "0.0.0.0"
                   :ssl-port    10080
                   :ssl-ca-cert "./dev-resources/ssl/ca.pem"
                   :ssl-cert    "./dev-resources/ssl/cert.pem"
                   :ssl-key     "./dev-resources/ssl/key.pem"
                   :client-auth "want"}}
      (testing "java sync client"
        (let [options (.. (RequestOptions. "https://localhost:10080/hello/")
                          (setSslCaCert "./dev-resources/ssl/ca.pem"))
              response (SyncHttpClient/get options)]
          (is (= 200 (.getStatus response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "clojure sync client"
        (let [response (sync/get "https://localhost:10080/hello/"
                                 {:ssl-ca-cert "./dev-resources/ssl/ca.pem"})]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response)))))))))

(deftest sync-client-test-with-invalid-ca-cert
  (testlogging/with-test-logging
    (testutils/with-app-with-config app
      [jetty9/jetty9-service test-web-service]
      {:webserver {:ssl-host    "0.0.0.0"
                   :ssl-port    10081
                   :ssl-ca-cert "./dev-resources/ssl/ca.pem"
                   :ssl-cert    "./dev-resources/ssl/cert.pem"
                   :ssl-key     "./dev-resources/ssl/key.pem"
                   :client-auth "want"}}
      (testing "java sync client"
        (let [options (.. (RequestOptions. "https://localhost:10081/hello/")
                          (setSslCaCert "./dev-resources/ssl/alternate-ca.pem"))]
          (try
            (SyncHttpClient/get options)
            ; fail if we don't get an exception
            (is (not true) "expected HttpClientException")
            (catch HttpClientException e
              (is (instance? SSLHandshakeException (.getCause e)))))))
      (testing "clojure sync client"
        (is (thrown? SSLHandshakeException
                     (sync/get "https://localhost:10081/hello/"
                               {:ssl-ca-cert "./dev-resources/ssl/alternate-ca.pem"})))))))
