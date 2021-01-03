(ns ch6.core-async
  (require [clojure.core.async :as async
            :refer [>! <! >!! <!!]]))

;; After seeing how we can interact with components either directly or asynchronously
;; through their api, we are now going to see how we can establish more enduring
;; interactions and relationship between components throughout the life of an application.

;; CONNECTING COMPONENTS WITH CHANNELS
;; Components might need to send values to each other introducing then the notion of a
;; producing component and consuming component. As seen previously, core-async channels
;; can be seen as a queue-like constructs allowing processes to send values to each other.

;; For e.g, a component that uses a channel to get its input can :
;; - receive an input channel as a parameter : this is the most `flexible` option since it
;; allows reuse of an externally-defined channel and broader control over the configuration
;; of the channel, for e.g : the size of the channel buffer.
;; - create an input channel internally : this option is more constrained. If the channel
;; has defined a buffer-size internally, it is baked-in and we have no control over the
;; configuration. In order to be able configure the internal channel, we have to
;; provide/inject configuration option to the component.

;; For the sake of example, let's take the case of a config processor.
;; First, let's define a processor to which we inject an externally-defined channel
(defn make-config-processor
  "A config processor receiving an externally-defined input channel as argument"
  [input-channel]
  (async/go
    (if-let [v (<! input-channel)]
      (println "Processing : " v))))

;; to use it, we proceed like so :
(let [in (async/chan 100) ;; we define a channel externally
      ;; bake the channel in the processor
      _ (make-config-processor in)]
  ;; finally, we push some random UUID values on the externally-defined channel
  (>!! in (java.util.UUID/randomUUID)))

;; > REPL output
;; Processing :  #uuid "b6454551-5bad-44bb-8ea3-41eaa0715edd"


;; Let's see now how we can take advantage of an internally-defined channel.
;; In this case, the processor actually exposes the channel by returning it from
;; the function definition.
;; The consumer function then uses it to pass the values in.
(defn make-config-alt-processor
  "A config processor using an internally-defined input channel (buf size 100)"
  []
  (let [input-channel (async/chan 100)] ; Notice that the buffer size is set internally
    ;; this go macro is the meat for the processor, doing the actual work of processing
    ;; the values extracted from the channel
    (async/go
      (if-let [v (<! input-channel)]
        (println "Processing from internal chan : " v)))
    ;; the input-channel is returned so that consumer can push values to it
    input-channel))

;; to use it, we can proceed like so
(let [input-channel (make-config-alt-processor)] ; since the input channel is returned by the fn
  (>!! input-channel (java.util.UUID/randomUUID)))

;; > REPL output
;; Processing from internal chan :  #uuid "b529c9ea-b6b6-4d6d-a988-dd780f31d532"

;;##################################################################################
;; core.async provides different types of channel connectors. We are going to review
;; them in terms of :
;; - direct connections
;; - fan in
;; - fan out

;; DIRECT CONNECTIONS
;; Two components having internally-defined channels can be connected together with
;; `pipe`.
;; Let's for e.g define two components, one producing a sequence, exposing an output
;; channel and another exposing an input channels to receive the elements generated
;; from those channels.

(defn make-name-processor
  "A processor internally producing a sequence of characters name"
  []
  (let [main-characters ["Sarah Connor" "John Connor" "Kyle Reeves"]
        output-channel (async/chan (async/dropping-buffer 10))]
    (async/go-loop [m main-characters]
      (>! output-channel m) ; we are sending character's name over the output channel
      (recur (rest main-characters)))
    output-channel)) ; we are exposing the output channel for any external components to pick up values from

(defn make-uppercase-processor
  "A processor acting as a sink, receiving elements from the first component and 
  upper-casing each"
  []
  (let [input-channel (async/chan (async/dropping-buffer 10))]
    (async/go
      (if-let [v (<! input-channel)]
        (println (clojure.string/upper-case v))))
    input-channel))

;; we can then `pipe` the output of the name processor to the splitter processor
(let [name-processor-out-channel (make-name-processor)
      name-uppercase-in-channel (make-uppercase-processor)]
  (async/pipe name-processor-out-channel name-uppercase-in-channel))

;; > REPL OUTPUT
;; ["SARAH CONNOR" "JOHN CONNOR" "KYLE REEVES"]


;; FAN OUT (ONE TO MANY)
;; core.async provides multiple abstractions for publishing messages on a channel
;; to many consumers. This allows multiple independent consumers to process messages
;; for different purposes.
;; There are 3 main constructs provided by the library :
;; - split : an abstraction allowing messages to be published on two different
;; channels based on a `predicate` function, a fn applied to each message allowing
;; it be put on either one of the channels depending on if the value of the message
;; satisfies the predicate or not.
;; - mult : an abstracton returning a multiple of the supplied channel. Channels
;; containing copies of the content of the supplied channel can be added to the
;; mult with `tap` and remove with `untap` : those channels will all
;; receive any item published to the mult. Since the distribution is synchronous and
;; in parallel, a buffering policy has to be set in order to avoid congestion if any of
;; the tap is not capable of keeping up with the pace when processing items.
;; - pub/sub : an abstraction like `mult` but allowing multiple consumers to `specialize`
;; by consuming messages on specialized partitions of the supplied channel called
;; `topic`s. Each topic corresponds to a certain type of message and unlike `mult`,
;; instead of all consumers receiving the same message, each consumer can now
;; explicitly express their intent to only be concerned with a certain type of
;; message published by the `pub` by `sub`-scribing to a topic.

;; Let's for e.g define a channel
(def detection-input-chan (async/chan (async/dropping-buffer 5)))

(def main-characters
  [{:firstname "John"
    :lastname "Connor"
    :type :human}
   {:model :t1000
    :type :terminator}
   {:firstname "Sarah"
    :lastname "Connor"
    :type :human}
   {:firstname "Kyle"
    :lastname "Reeves"
    :type :human}
   {:model :tx
    :type :terminator}])

;; Let's use a `split` to separate the humans from the machines. We then need
;; to provide a predicate to the split. This predicate will return true if the
;; entity is a :human and false otherwise (including when it is a :terminator).

(defn human?
  [entity]
  (= :human (:type entity)))

(defn split-channel
  "Splits a channel based on whether an entity is of :type :human or :terminator
  and returns the two resulting channels corresponding to each type in a vector"
  [input-chan]
  (async/split human? input-chan (async/dropping-buffer 1) (async/dropping-buffer 1)))

;; We now need to set up the input channel `split`-ing and the receiving processes
(let [[human-chan machine-chan] (split-channel detection-input-chan)]
  (async/go-loop []
    (println "Human identified : " (:firstname (<! human-chan)))
    (recur))

  (async/go-loop []
    (println "Intruder detected : " (:model (<! machine-chan)))
    (recur)))

;; And then finally, we need to feed the values to the processes via the split channel
(async/go
  (doseq [mc main-characters]
    (println "Sending : " mc)
    (>! detection-input-chan mc)))

;; > REPL OUTPUT
;; Sending :  {:firstname John, :lastname Connor, :type :human}
;; Sending :  {:model :t1000, :type :terminator}
;; Human identified :  John
;; Sending :  {:firstname Sarah, :lastname Connor, :type :human}
;; Intruder detected :  :t1000
;; Sending :  {:firstname Kyle, :lastname Reeves, :type :human}
;; Human identified :  Sarah
;; Sending :  {:model :tx, :type :terminator}
;; Human identified :  Kyle
;; Intruder detected : (model)  :tx
;; user>


