(ns ch6.core-async
  (require [clojure.core.async :as async
            :refer [>! <!]]))

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


