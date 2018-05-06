(ns ch2.orders)

;; A Queue is the right collection implementation for an ordering system
;; where the first item IN is the first item OUT to be processed in the pipeline.
;; It is both efficient for adding (at the end) and removing (at the front)
;; the orders to be processed in the order they were introduced in the system,
;; that is in a FIFO fashion.

;; There is no literal syntax or constructor associated with Queues.
;; Instead we use a static call which returns an empty queue.
(def new-orders clojure.lang.PersistentQueue/EMPTY)

(defn add-order
  [orders order]
  ;; Adding an order simply involves calling the 'conj' function. The Polymorphism
  ;; machinery then takes care of using the right and most efficient implementation
  ;; for adding the element to the collection based on this latter underlying concrete type.
  ;; 'conj' adds elements at a collection's natural insertion point which is the point where
  ;; adding an element for that particular data structure is the most efficient. For
  ;; example, for a list the natural insertion point is at the beginning whereas for
  ;; a vector, it is at the end.
  (conj orders order))

;; A hypothetical cooking process for the sake of illustration
(defn cook
  [order]
  (println "Cooking..."))

;; This method retrieves the first order introduced in the ordering system,
;; sends it to the processing pipeline first and then returns the remaining orders
;; to be processed next
(defn cook-order
  [orders]
  ;; Here is another great example of type polymorphism. Depending on the type of the
  ;; underlying collection, 'peek' have the same behaviour as 'first' in lists and queues
  ;; but behaves like 'last' for vectors
  (cook (peek orders))
  (pop orders))

