(ns ch5.jqueue
  (:import [java.util.concurrent LinkedBlockingQueue]))

;; JAVA QUEUES
;; Java offers multiple queues and workers implementation in its java.util.concurrent
;; package. In particular, all implementations of java.util.concurrent.BlockingQueue
;; offers `blocking` and are available for use within Clojure right away through 
;; java interop calls.

;; The main difference among the java queue implementations is in how they buffer data.
;; - LinkedBlockingQueue provides an optionally bounded buffer.
;; - ArrayBlockingQueue provides a bounded buffer.
;; - SynchronousQueue does not provide buffer at all hence forcing the procudeer and
;; consumer to wait until both are ready to proceed with the value handout.
;; - LinkedTransferQueue has the hand-off capabilities of SynchronousQueue but with an
;; optional bounded buffer.
;; All of those queues seen so far are FIFO queues

;; But Java also provides two queues that can reorder items.
;; - PriorityBlockingQueue which bubbles up elements having higher priorities to the front
;; of the queue
;; - DelayQueue which makes messages available after the provided delay expires.

;; Let's again illustrate it with our terminator order queues. We are going to create
;; two process : a consumer and a popper that will be executed on a background thread pool
(defn pusher
  "Push values from 0 to n on the queue"
  [q n]
  (loop [i 0]
    (when (< i n)
      (.put q i)
      (recur (inc i))))
  (.put q :end))

(defn popper
  "Pop values from the queue until it encounters :end"
  [q]
  (loop [items []]
    (let [item (.take q)]
      (if (= :end item)
        items
        (recur (conj items item))))))

(defn flow
  [n]
  (let [q (LinkedBlockingQueue.)
        ;; we run the consumer on the background thread pool
        consumer (future (popper q))
        begin (System/currentTimeMillis)
        ;; we run the producer on the background thread pool as well
        producer (future (pusher q n))
        ;; we wait `block`-ing on the consumer waiting for the hand-off from
        ;; the producer to finish
        received @consumer
        end (System/currentTimeMillis)]
    (println "Received " (count received) " in " (- end begin) " ms")))

;; Let's run it to check if we actually received the 15 items
(flow 15)
;; > Received  15  in  0  ms
