(ns puppetlabs.http.client.common
  (:refer-clojure :exclude (get)))

(defprotocol HTTPClient
  (get [this url] [this url opts])
  (head [this url] [this url opts])
  (post [this url] [this url opts])
  (put [this url] [this url opts])
  (delete [this url] [this url opts])
  (trace [this url] [this url opts])
  (options [this url] [this url opts])
  (patch [this url] [this url opts])
  (close [this]))
