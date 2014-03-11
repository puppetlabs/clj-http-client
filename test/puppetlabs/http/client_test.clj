(ns puppetlabs.http.client-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [resource]] 
            [puppetlabs.http.client :as http])
  (:import [javax.net.ssl SSLEngine]))

(deftest ssl-config-with-files
  (let [req {:url "http://localhost"
             :method :get
             :ssl-cert (resource "resources/cert.pem")
             :ssl-key (resource "resources/key.pem")
             :ssl-ca-cert (resource "resources/ca.pem")}
        configured-req (http/configure-ssl req)]

    (testing "configure-ssl sets up an SSLEngine when given cert, key, ca-cert"
      (is (instance? SSLEngine (:sslengine configured-req))))

    (testing "removes ssl-cert, ssl-key, ssl-ca-cert"
      (is (not (:ssl-cert configured-req)))
      (is (not (:ssl-key configured-req)))
      (is (not (:ssl-ca-cert configured-req))))))

(deftest ssl-config-without-files
  (let [req {:url "http://localhost"
             :method :get}
        configured-req (http/configure-ssl req)]

    (testing "configure-ssl does nothing when given no files"
      (is (= req configured-req)))))
