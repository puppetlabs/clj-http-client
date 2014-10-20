(ns com.puppetlabs.http.client.impl.async-close-test
  (:import (java.io Closeable)
           (com.puppetlabs.http.client.impl AsyncClose))
  (:require [clojure.test :refer :all]))

(defn closeable
  [closeable-promise]
  (reify Closeable
    (close [this]
      (deliver closeable-promise (Thread/currentThread)))))

(deftest closeable-asynchronously-closed
  (testing "Close called asynchronously on a closeable"
    (let [closeable-promise (promise)
          closeable         (closeable closeable-promise)]
      (AsyncClose/close closeable)
      (is @closeable-promise "Promise realized as nil")
      (is (instance? Thread @closeable-promise) "Thread not delivered")
      (is (not (identical? (Thread/currentThread) @closeable-promise))
          "Closeable closed from originating thread, not asynchronously"))))


