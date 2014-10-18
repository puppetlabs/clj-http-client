(ns puppetlabs.http.client.test-common
  (:require [ring.middleware.params :as ring-params]
            [puppetlabs.trapperkeeper.core :as tk]))

(defn query-params-test
  [req]
  {:status 200
   :body (str (:query-params req))})

(def app-wrapped
  (ring-params/wrap-params query-params-test))

(tk/defservice test-params-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler app-wrapped "/params")
        context))

(def queryparams {"foo" "bar"
                  "baz" "lux"})

(def query-options {:method       :get
                    :url          "http://localhost:8080/params/"
                    :query-params queryparams
                    :as           :text})

(defn redirect-test-handler
  [req]
  (condp = (:uri req)
    "/hello/world" {:status 200 :body "Hello, World!"}
    "/hello"       {:status 302
                     :headers {"Location" "/hello/world"}
                     :body    ""}
    {:status 404 :body "D'oh"}))

(tk/defservice redirect-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler redirect-test-handler "/hello")
        context))