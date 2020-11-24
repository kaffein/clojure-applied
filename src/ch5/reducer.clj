(ns ch5.reducer)

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
