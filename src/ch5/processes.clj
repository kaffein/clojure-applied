(ns ch5.processes
  (:require [clojure.core.async :refer [chan dropping-buffer sliding-buffer go
                                        >!! <!! >! <!]]))

;; As of now, we have seen how to to :
;; - one-time asynchrony (futures, promises)
;; - coarse-grained paralellism with the ExecutorService API
;; - fine-grained paralellism via Clojure's reducers and the Fork/join Framework

;; Sometimes though, we are interested in the concurrent nature of the processing
;; than the parallelism. That is having multiple concurrent threads with the ability
;; to convey values between those threads.
;; The clojure.core-async library has been created to address those needs. It provides
;; two main abstractions :
;; - go blocks : independent threads of execution
;; - channels : a way to pass values from one thread to another

;; CHANNELS
;; channels are queue-like construcs for conveying values from one thread to another.
;; They are created and passed around between threads. They are `stateful` and all values
;; put on the channel is conveyed to the other side except `nil` which indicates that the
;; channel was `closed`. The channel can be close with the `close!` function.

;; Channels use `buffer` to hold values within the channel. By default, channels have are
;; unbuffered. Unbuffered channels block until both a producer and consumer are available
;; to hand a value accross the channel.
;; core.async also provides some variation of buffered channels :
;; - fixed-length buffers
;; - dropping-buffers : values put on the channels are dropped if the buffer is full
;; - sliding-buffers ; values that have been put first on the channels are dropped if the
;; buffer is full (FIFO)

;; to create an unbuffered channel
(chan)
;; to create a buffered channel
(chan 10)
;; to create a dropping buffer
(chan (dropping-buffer 10))
;; to create a sliding buffer
(chan (sliding-buffer 10))


;; OPERATIONS ON CHANNELS
;; The two most important operations that can be performed on a channel are : 
;; - put
;; - take
;; Those two operations have two variants `blocking` and `non-blocking` depending on the
;; context of their use.

;; When used in ordinary threads, the put operation is `>!!` : the double exclamation mark
;; meaning the operation is `side-effect`-y and `blocking`. The take operation on the other
;; side of the channel is `<!!`.

(def c (chan 1)) ;; notice the buffer size 1
(>!! c "hello")
(println (<!! c))
;; REPL-output
;; hello
;; user>

;; here we put a buffer size 1 to avoid the blocking of the channel. In fact, the put operation
;; would have blocked since there were no process yet to do the take operation on the other side.

;; Channels in core async are never unbounded to prevent some part of the system to
;; pile up with values, eventually leading it to crash. It is a design choice allowing for e.g
;; the system to make adjustments on upstream producers if the consumers downstream can not
;; cope up while processing. (ref.`backpressure`).
;; Instead, you are required to specify a size for buffers.

;; Even though, it is possible to use channels directly from ordinary threads, core.async
;; has a construct called `go-blocks` which allows the creation of lightweight processes
;; backed by a pool of threads.


;; GO BLOCKS
;; go blocks are lightweight processes that are backed by a thread pool and which are run
;; only when there is work to be done.
;; Instead of blocking on a channel for new messages, those processes are instead `parked` 
;; and woken up when a message is put on the channel allowing the go block to proceed with
;; the processing of the value taken from the channel.
;; Unlike tradition threads, they do not consume resources when they are parked.

;; When using channels from inside go-blocks, we use the non-blocking variant of `put` and
;; `take` : `>!` and `<!`. (notice the single exclamation mark)

;; Let's implement a printing go-block for the sake of example :
(defn go-print
  "Prints the value sent to the channel"
  [c]
  (go
    (loop []
      (when-some [value (<! c)] ;; parked triggered if the channel is empty
        (println value)
        (recur)))))

;; When the go-block encounters the call to take from the channel but not value has been
;; put on it yet, the go-block is parked

(def c (chan 1))
;; running the go-block with the channel above provided as param
(go-print c)
;; putting a value on the channel : notice that we had to use the blocking variant here
;; since we are doing it from an ordinary thread
(>!! c "I'll be back!")
;; REPL-output
;; I'll be back!
;; user> 
