(ns puppetlabs.http.client.persistent-async
  (:import (org.apache.http.impl.nio.client HttpAsyncClients))
  (:require [schema.core :as schema]
            [puppetlabs.http.client.common :as common]
            [puppetlabs.http.client.schemas :as schemas]
            [puppetlabs.http.client.async :refer [request configure-ssl]])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn create-client :- common/HTTPClient
  [opts :- schemas/ClientOptions]
  (let [opts    (configure-ssl opts)
        client  (if (:ssl-context opts)
                  (.. (HttpAsyncClients/custom) (setSSLContext (:ssl-context opts)) build)
                  (HttpAsyncClients/createDefault))]
    (.start client)
    (reify common/HTTPClient
      (get [this url] (common/get this url {}))
      (get [_ url opts] (request (assoc opts :method :get :url url) nil client))
      (head [this url] (common/head this url {}))
      (head [_ url opts] (request (assoc opts :method :head :url url) nil client))
      (post [this url] (common/post this url {}))
      (post [_ url opts] (request (assoc opts :method :post :url url) nil client))
      (put [this url] (common/put this url {}))
      (put [_ url opts] (request (assoc opts :method :put :url url) nil client))
      (delete [this url] (common/delete this url {}))
      (delete [_ url opts] (request (assoc opts :method :delete :url url) nil client))
      (trace [this url] (common/trace this url {}))
      (trace [_ url opts] (request (assoc opts :method :trace :url url) nil client))
      (options [this url] (common/options this url {}))
      (options [_ url opts] (request (assoc opts :method :options :url url) nil client))
      (patch [this url] (common/patch this url {}))
      (patch [_ url opts] (request (assoc opts :method :patch :url url) nil client))
      (close [_] (.close client)))))