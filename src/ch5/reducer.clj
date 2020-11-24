(ns ch5.reducer
  (:require [clojure.core.reducers :as r]))

;; JAVA LEGACY
;; Prior to Java 5, solving concurrency problems on the Java platform consisted in
;; working with low-level apis such as Threads, locks etc. Not only were those low-level
;; apis complex but also error-prone. The developer could not focus on solving the problem
;; at hand but instead had to deal with the technical details inherent to those low-level
;; apis.

;; With the rise of multi-core CPUs, the version 5 of the Java platform came out with a new
;; higher-level api for dealing with concurrency : the `ExecutorService`.
;; The api consists in having a thread pool to which tasks are submitted in a queue
;; to be run or executed in parallel by a bunch of worker processes.
;; In terms of developer experience, the new api was a good compromise for usability.
;; The only issue was that, the api was adapted for coarse-grained tasks. Also, as the
;; number of cores increased in CPUs, having to wait on a single queue became a bottleneck
;; for worker processes retrieving items for the queue.

;; To remedy this issue, a new api was also introduced when Java 7 came out :
;; `Fork/join framework`. This new api was well adapted for small fine-grained computational
;; tasks, recursive computation on a high number of cores.
;; The api consists in having multiple queues for submitting tasks and to which a number
;; of worker threads pop tasks from. What makes this api special though is that each queues
;; can `steal` items from one another avoiding starving processes having to wait for new
;; task items if they have exhausted their queues.

;; Clojure provides a framework that leverages the use of the `Fork/join` in a way that is
;; more natural to Clojure developers.



;; PARALLELISM WITH REDUCERS
;; Data manipulation in Clojure consists in applying a function to sequence elements. A 
;; sequence is a logical list of values in some order.
;; Most of the clojure core libraries fns for data transformation though are applied lazily, 
;; in order and single-threaded.
;; `Reducers` are an alternative way to apply those data transformations while also having
;; the benefits of doing so `eagerly`, without the need for intermediate collections
;; but also taking advantage of the Fork/Join framework to execute those fns in parallel.
;; A `reducer` consists of a `reducible collection` and a `reducing function`.
;; - a reducible collection is a collection that can efficiently perform a reduce fn on
;; itself
;; - a reducer fn is a function that knows how to accumulate a result during a reduce op

;; Let's take the example of a simple shipping service :

(def data
  [{:id 1
    :class :ground
    :weight 10
    :volume 600}
   {:id 2
    :class :ground
    :weight 60
    :volume 220}
   {:id 3
    :class :air
    :weight 1
    :volume 20}
   {:id 4
    :class :ground
    :weight 6
    :volume 60}])

(defn ground?
  [product]
  (= :ground (:class product)))

(defn ground-weight
  "Compute the total weight for ground-fret"
  [products]
  (->> products
       (filter ground?)
       (map :weight)
       (reduce +)))

(ground-weight data)

;; We express our program in terms of function composition however this code will only
;; use a single thread to do the work.
;; The `map` function has an equivalent `pmap` allowing it to be run in parallel by
;; sending each element to a future. The issue with this approach is that the overhead
;; introduced by the creation of a thread and the synchronization due to thread boundary
;; for retrieving the returned value from the worker thread comes at a higher cost 
;; relatively to the computation to be performed (here extracting :weight). In those cases,
;; the parallel version of the fn is often more expensive than its single-threaded counterpart.

;; With reducers, we can express the same semantic as our single-threaded program by
;; composing functions but in addition to that we have the ability to run them in parallel.
;; In fact, the reducers library even has the equivalent of the functions we just invoked
;; (map, filter, reduce) in the single-threaded version of the code.
;; The only difference is that instead of creating the intermediate collection for each
;; transformation in the pipeline, reducers version of each of those functions actually
;; return a reducer, which will then stack up as the go down the pipeline. It is like if
;; they form a unique stack of composed operation taking into account every function along
;; the way in the pipeline. Those functions do not perform the transformations.
;; Instead, we have to invoke a reduce-like operation called `fold` to trigger the trans-
;; formation. Nothing happens until that final call to `fold`.

;; Let's rewrite the clojure.core based transformation above with reducers :
(defn reducer-ground-weight
  "Compute the total weight for ground-fret (reducers version)"
  [products]
  (->> products
       (r/filter ground?)
       (r/map :weight)
       (r/fold +))) ;; trigger the transformation

(reducer-ground-weight data)
;; we have the same exact result as before even though the advantage of having parallelism
;; is not obvious here because the data size is small.
;; In fact, the mechanism underlying the parallelism works by dividing the input data
;; in chunks of arbitrary size N and applying the reduce function on those chuncks before
;; finally combining the results of each to get the final result.
;; This partition size N acts as a threshold for triggering the parallelization mechanism when
;; a `fold` terminal operation is invoked.
;; It is only when the input data size x > N that the input data chunking and hence computation
;; parallelization is triggered.
;; By default, N = 512.

;; In our case, chunking the data and parallelizing the computation is an overhead given the
;; input data size and so it just falls back to a single partition data consisting of everything
;; we have inthe input data.

