(ns puppetlabs.http.client.sync-ssl-test
  (:require [clojure.test :refer :all]
            [puppetlabs.http.client.sync :as sync]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :as jetty9]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as testutils]
            [puppetlabs.trapperkeeper.testutils.logging :as testlogging]
            [schema.test :as schema-test])
  (:import (com.puppetlabs.http.client HttpClientException
                                       SimpleRequestOptions
                                       Sync)
           (com.puppetlabs.ssl_utils SSLUtils)
           (java.net URI)
           (javax.net.ssl SSLException SSLHandshakeException)
           (org.apache.http ConnectionClosedException)))

(use-fixtures :once schema-test/validate-schemas)

(defn app
  [_req]
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
        (let [request-options (.. (SimpleRequestOptions. (URI. "https://localhost:10080/hello/"))
                                  (setSslCert "./dev-resources/ssl/cert.pem")
                                  (setSslKey "./dev-resources/ssl/key.pem")
                                  (setSslCaCert "./dev-resources/ssl/ca.pem"))
              response (Sync/get request-options)]
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
        (let [request-options (.. (SimpleRequestOptions. (URI. "https://localhost:10080/hello/"))
                                  (setSslCaCert "./dev-resources/ssl/ca.pem"))
              response (Sync/get request-options)]
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
        (let [request-options (.. (SimpleRequestOptions. (URI. "https://localhost:10081/hello/"))
                                  (setSslCaCert "./dev-resources/ssl/alternate-ca.pem"))]
          (try
            (Sync/get request-options)
            ; fail if we don't get an exception
            (is (not true) "expected HttpClientException")
            (catch HttpClientException e
              (if (SSLUtils/isFIPS)
                ;; in FIPS, the BC provider throws a different exception here, specifically:
                ;; javax.net.ssl.SSLException: org.bouncycastle.tls.TlsFatalAlert: certificate_unknown(46)
                (is (instance? SSLException (.getCause e)))
                (is (instance? SSLHandshakeException (.getCause e))))))))
      (testing "clojure sync client"
        (if (SSLUtils/isFIPS)
          ;; in FIPS, the BC provider throws a different exception here, specifically:
          ;; javax.net.ssl.SSLException: org.bouncycastle.tls.TlsFatalAlert: certificate_unknown(46)
          (is (thrown? SSLException
                       (sync/get "https://localhost:10081/hello/"
                                 {:ssl-ca-cert "./dev-resources/ssl/alternate-ca.pem"})))
          (is (thrown? SSLHandshakeException
                       (sync/get "https://localhost:10081/hello/"
                                 {:ssl-ca-cert "./dev-resources/ssl/alternate-ca.pem"}))))))))

(defmacro with-server-with-protocols
  [server-protocols server-cipher-suites & body]
  `(testlogging/with-test-logging
    (testutils/with-app-with-config app#
      [jetty9/jetty9-service test-web-service]
      {:webserver (merge
                    {:ssl-host      "0.0.0.0"
                     :ssl-port      10080
                     :ssl-ca-cert   "./dev-resources/ssl/ca.pem"
                     :ssl-cert      "./dev-resources/ssl/cert.pem"
                     :ssl-key       "./dev-resources/ssl/key.pem"
                     :ssl-protocols ~server-protocols}
                    (if ~server-cipher-suites
                      {:cipher-suites ~server-cipher-suites}))}
      ~@body)))

(defmacro java-unsupported-protocol-exception?
  [& body]
  `(try
     ~@body
     (catch HttpClientException e#
       (let [cause# (.getCause e#)
             message# (.getMessage cause#)]
         (or (and (instance? SSLHandshakeException cause#)
                  (or ;; java 11
                      (re-find #"protocol_version" message#)
                      ;; java 8
                      (re-find #"not supported by the client" message#)))
             (and (instance? SSLException cause#)
                  (or (re-find #"handshake_failure" message#)
                      (re-find #"internal_error" message#)))
             (instance? ConnectionClosedException cause#))))
     (catch ConnectionClosedException cce# true)))

(defn java-https-get-with-protocols
  [client-protocols client-cipher-suites]
  (let [request-options (.. (SimpleRequestOptions. (URI. "https://localhost:10080/hello/"))
                            (setSslCert "./dev-resources/ssl/cert.pem")
                            (setSslKey "./dev-resources/ssl/key.pem")
                            (setSslCaCert "./dev-resources/ssl/ca.pem"))]
    (when client-protocols
      (.setSslProtocols request-options (into-array String client-protocols)))
    (when client-cipher-suites
      (.setSslCipherSuites request-options (into-array String client-cipher-suites)))
    (Sync/get request-options)))

(defn clj-https-get-with-protocols
  [client-protocols client-cipher-suites]
  (let [ssl-opts (merge {:ssl-cert    "./dev-resources/ssl/cert.pem"
                         :ssl-key     "./dev-resources/ssl/key.pem"
                         :ssl-ca-cert "./dev-resources/ssl/ca.pem"}
                        (when client-protocols
                          {:ssl-protocols client-protocols})
                        (when client-cipher-suites
                          {:cipher-suites client-cipher-suites}))]
    (sync/get "https://localhost:10080/hello/" ssl-opts)))

(deftest sync-client-test-ssl-protocols
  (testing "should be able to connect to a TLSv1.2 server by default"
    (with-server-with-protocols ["TLSv1.2"] nil
      (testing "java sync client"
        (let [response (java-https-get-with-protocols nil nil)]
          (is (= 200 (.getStatus response)))
          (is (= "Hello, World!" (slurp (.getBody response))))))
      (testing "clojure sync client"
        (let [response (clj-https-get-with-protocols nil nil)]
          (is (= 200 (:status response)))
          (is (= "Hello, World!" (slurp (:body response))))))))

  (testing "should not connect to a server when protocols don't overlap"
    (with-server-with-protocols ["TLSv1.1"] ["TLS_RSA_WITH_AES_128_CBC_SHA"]
      (testing "java sync client"
        (is (java-unsupported-protocol-exception?
              (java-https-get-with-protocols ["TLSv1.2"] nil))))
      (testing "clojure sync client"
        (is (java-unsupported-protocol-exception? (clj-https-get-with-protocols ["TLSv1.2"] nil)))))))

