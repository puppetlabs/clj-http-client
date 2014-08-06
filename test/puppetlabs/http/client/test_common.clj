(ns puppetlabs.http.client.test-common
  (:require [ring.middleware.params :as ring-params]
            [puppetlabs.trapperkeeper.core :as tk]))

(defn query-params-test
  [req]
  {:status 200
   :body (str (:params req))})

(def app-wrapped
  (ring-params/wrap-params query-params-test))

(tk/defservice test-params-web-service
  [[:WebserverService add-ring-handler]]
  (init [this context]
        (add-ring-handler app-wrapped "/params")
        context))

(def queryparams {"yellow"  "submarine"
                  "eleanor" "rigby"})

(def query-options {:method :get
                    :url    "http://localhost:8080/params/"
                    :query-params queryparams
                    :as :text})