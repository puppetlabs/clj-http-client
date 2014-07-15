(ns puppetlabs.http.client.persistent-async
  (:import (org.apache.http.impl.nio.client HttpAsyncClients))
  (:require [schema.core :as schema]
            [puppetlabs.http.client.schemas :as schemas]
            [puppetlabs.http.client.async :refer [request configure-ssl]])
  (:refer-clojure :exclude (get)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Async Client protocol

(defprotocol AsyncClient
  (get [this url] [this url opts])
  (head [this url] [this url opts])
  (post [this url] [this url opts])
  (put [this url] [this url opts])
  (delete [this url] [this url opts])
  (trace [this url] [this url opts])
  (options [this url] [this url opts])
  (patch [this url] [this url opts])
  (close [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(schema/defn create-client :- AsyncClient
  [opts :- schemas/ClientOptions]
  (let [opts    (configure-ssl opts)
        client  (if (:ssl-context opts)
                  (.. (HttpAsyncClients/custom) (setSSLContext (:ssl-context opts)) build)
                  (HttpAsyncClients/createDefault))]
    (.start client)
    (reify AsyncClient
      (get [this url] (get this url {}))
      (get [_ url opts] (request (assoc opts :method :get :url url) nil client))
      (head [this url] (head this url {}))
      (head [_ url opts] (request (assoc opts :method :head :url url) nil client))
      (post [this url] (post this url {}))
      (post [_ url opts] (request (assoc opts :method :post :url url) nil client))
      (put [this url] (put this url {}))
      (put [_ url opts] (request (assoc opts :method :put :url url) nil client))
      (delete [this url] (delete this url {}))
      (delete [_ url opts] (request (assoc opts :method :delete :url url) nil client))
      (trace [this url] (trace this url {}))
      (trace [_ url opts] (request (assoc opts :method :trace :url url) nil client))
      (options [this url] (options this url {}))
      (options [_ url opts] (request (assoc opts :method :options :url url) nil client))
      (patch [this url] (patch this url {}))
      (patch [_ url opts] (request (assoc opts :method :patch :url url) nil client))
      (close [_] (.close client)))))