(ns ch5.queue)

;; All programs can be considered in whole or in part as task processors.
;; For e.g : a web service receiving requests to process api calls. A batch
;; program reading files from disk or db and processing each one appropriately.
;; All of these common patterns can be modeled as queue of work farmed out to
;; a pool of workers reponsible for completing the tasks.
;; The queue holds and stores the tasks thus decoupling the place where work
;; arrives from where it is processed. The worker pool allows the control and
;; monitoring of the amount of concurrency that we would like to have in order
;; to execute the tasks while taking advantage and optimizing the use of hardware
;; resources at our disposal.
;; Clojure does not reinvent the wheel and makes use of the plethora of tools
;; available on the JVM for queues and workers.

;; As seen earlier, we could for for e.g use Clojure's persistent queues
;; which are more efficient than vectors and lists to access data in a FIFO
;; fashion.
;; In the case of a persistent queue, as it is immutable, we would get a new
;; queue each time we add or remove an element to/from it. It would be perfectly
;; okay in a single-threaded environment but as soon as we have many threads
;; accessing the shared queue we need a way to manage it in a stateful way using the
;; tools seen previously with `reference types`.
;; We can then either wrap our queue in :
;; - an `atom`
;; - or in a `ref`

;; with an `atom`, applying the Unified update model would give us a new
;; immutable queue each time we make a modification either by adding or removing
;; an element to/from it. There is no way to get the element that has been popped out
;; from the queue.
(clojure.repl/doc swap!)

;; using `ref` on the other hand can be more convenient in that we can provide a context
;; via the transaction delimitation where we can access elements from within the queue as in the
;; following example (c.f `dequeue` below).
(defn queue
  "Create a new stateful queue"
  []
  (ref clojure.lang.PersistentQueue/EMPTY))

(defn enqueue
  "Enqueue an item in the queue"
  [q item]
  (dosync
   (alter q conj item)))

(defn dequeue
  "Dequeue an item from the queue"
  [q]
  (dosync
   (let [item (peek @q)]
     (alter q pop)
     item)))

;; Let's get back to our terminator example. Suppose Skynet uses a queue to
;; insert kill orders
(def kill-orders (queue))

(enqueue kill-orders "Terminate Sarah Connor")
(enqueue kill-orders  "Terminate John Connor")

;; Let's make sure that the kill orders have been added to the queues
(count @kill-orders)
;; This should return 2

;; Let's test whether the dequeueing process works since our terminator workers will
;; proceed this way in order to receive their mission orders.
(dequeue kill-orders)
;; > "Terminate Sarah Connor"

(dequeue kill-orders)
;; > "Terminate John Connor"

;; at this point, the queue is now empty since we depleted the orders from the queue
;; the following will then return nil.
(dequeue kill-orders)
;; > nil

;; Our order queue implementation is then `NOT blocking` which means that for workers
;; to be aware that there are some orders to be taken, they have to regularly poll the
;; queue. What we would like to have instead is that the workers block on the queue
;; waiting for new data to arrive instead of having to repeteadly poll it.
;; Fortunately the JVM has a plethora of implementations for queues and workers allowing
;; this kind of behaviour.
