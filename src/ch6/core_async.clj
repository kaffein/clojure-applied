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

;; We now need to split up the input channel using `split` and also set up
;; the receiving processes
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


;; Let's now suppose that instead of having some specialized components up front,
;; the Resistance has some internal components that need to receive all information
;; regarding any entity that enters its territory.
;; For the sake of example, let's say that :
;; - one component is responsible for logging all info regarding an entity entering
;; the Resistance territory.
;; - another one could be responsible for triggering an alert if a :terminator has
;; entered the zone
;; - and finally, the last one could be used to count the number of entity having
;; entered the territory.

;; Since those components need to apply different processing to the entirety of the
;; messages from the input channel, they all need the copies of information
;; received by the input channel. Thus the use of `mult` allows us to provide
;; multiple channels that will then receive those messages and will be used as the
;; input channels of those components.

;; Let's define the main input channel on which the messages will be put up in
;; the stream
(def main-chan (async/chan (async/dropping-buffer 2)))

;; To receive the messages from the main channel, the 3 downstream channels have to
;; `tap` into the `mult` defined on it.

(defn mult-channel
  "Mults a channel and returns the downstream channels for processors to consume"
  [input-chan]
  (let [m (async/mult input-chan)
        logging-chan (async/tap m (async/chan (async/dropping-buffer 2)))
        alerting-chan (async/tap m (async/chan (async/dropping-buffer 2)))
        counting-chan (async/tap m (async/chan (async/dropping-buffer 2)))]
    [logging-chan alerting-chan counting-chan]))

(let [[logging-chan alerting-chan counting-chan] (mult-channel main-chan)]
  ;; let's define 3 simple processes for those downstream channels
  (async/go-loop []
    (println "Logging proc : " (<! logging-chan))
    (recur))

  (async/go-loop []
    (println "alerting proc : " (<! alerting-chan))
    (recur))

  (async/go-loop []
    (println "Counting proc : " (<! counting-chan))
    (recur)))

(async/go
  (doseq [mc main-characters]
    (println "Sending to mult : " mc)
    (>! main-chan mc)))

;; > REPL OUTPUT : we can see that the 3 processors receive the same messages
;; Sending to mult :  {:model :t1000, :type :terminator}
;; Sending to mult :  {:firstname Kyle, :lastname Reeves, :type :human}

;; ... (removed since some log lines were interwined due to the concurrent nature of ops
;; Logging proc :  {:model :t1000, :type :terminator}
;; alerting proc :  {:model :t1000, :type :terminator}
;; Counting proc :  {:model :t1000, :type :terminator}
;; ...

;; IMPORTANT : Notice the use of `dropping-buffer` channels for the downstream
;; channels. It is important since it allows the downstream processes to not be
;; overwhelmed with even more messages if it can not keep up with its processing.
;; This will allow the system to continue working and not crash.


;; Let's finally consider the case where a channel wants to `tap` into an upstream channel
;; but only to receive messages that are appropriate for its usage, instead of receiving 
;; everything like with the previous example and having the downstream processor doing the
;; filtering of the messages.
;; core.async provides a `pub/sub` abstraction that allows just that. In this scheme, an
;; input channel is `publish`-ed based on a `topic-fn`. The `topic-fn` is a function applied
;; to the message and allowing the system to determine what `topic` the flowing message belongs
;; to before publishing the message on the `topic`. The `topic` is then like a partitioned message
;; queue which is `specialized` in one type of message.
;; 
;; On the downstream side, one or many channels `sub`-sribe to those `topic`s and receive
;; any message that are published by the upstream channel to that particular topic.

;; For the sake of example, let's say that we now want to be able to push incoming messages
;; to topics based on their :type (either :human, :terminator or :unknown) and have
;; channels specialized and thus subscribing to those `topic`s.

;; let's define a new type of entity and a new upstream channel
(def aliens
  [{:name "ET"
    :type :unknown}
   {:name "Predator"
    :type :unknown}])

(def entity-chan (async/chan (async/dropping-buffer 2)))

;; like in the previous example, let's create a pub from an upstream input channel and
;; have a 3 subscribing channels.

(defn sub-channel
  "Subs a main channel and returns the downstream subs for processors to consume"
  [input-chan]
  (let [p (async/pub input-chan :type)
        human-chan (async/sub p :human (async/chan (async/dropping-buffer 2)))
        machine-chan (async/sub p :terminator (async/chan (async/dropping-buffer 2)))
        unknown-chan (async/sub p :unknown (async/chan (async/dropping-buffer 2)))] ;; we discriminate on :type since keywords are fn in Clojure
    [human-chan machine-chan unknown-chan]))

;; Let's now create the processors for those topic channels
(let [[human-chan machine-chan unknown-chan] (sub-channel entity-chan)]
  (async/go-loop []
    (println "Human identified : " (:firstname (<! human-chan)))
    (recur))

  (async/go-loop []
    (println "Hostile entity identified : " (:model (<! machine-chan)))
    (recur))

  (async/go-loop []
    (println "Unknown entity detected : " (:name (<! unknown-chan)))
    (recur)))

;; Finally, we feed the data as usual to the upstream input channel
(async/go
  (doseq [mc (into main-characters aliens)]
    (>! entity-chan mc)))

;; > REPL OUTPUT
;; Human identified :  John
;; Human identified :  Kyle
;; Human identified :  Sarah
;; Hostile entity identified :  :t1000
;; Unknown entity detected :  ET
;; Hostile entity identified :  :tx
;; Unknown entity detected :  Predator
;; user>

;; every processor is now specialized and only receives an entity of the kind it is interested in.


;; FAN-IN (MANY-TO-ONE)
;; In addition to fan-out, core.async also provides support for `fan-in` i.e the ability
;; to aggregate multiple channels into one. There are two abstractions available in the lib :
;; - `merge` : allows multiple channels traffic to be combined into a single linear output stream.
;; A merged channel can not be modified after it has been created.
;; - `mix` : allows for more sophisticated use cases. In fact, it is possible to control the
;; multiple input channels by `pausing`, `muting/unmuting` or `soloing` each independently using
;; the `toggle` function. It is also to set the  `solo-mode` (either :mute or :pause)
;; on the output channel allowing the solo-ed input channels to be muted or paused.

;; Let's say for e.g that we have three channels as upstream channels, each streaming
;; specific data based on their :type (they could have been retrieved from `topics`)

(def machine-chan (async/chan (async/dropping-buffer 2)))
(def human-chan (async/chan (async/dropping-buffer 2)))
(def unknown-chan (async/chan (async/dropping-buffer 2)))

;; we are going to merge those channels into an output channel, from which we are going
;; to extract values. This time, we will use functions to make the combination and to
;; show that defining them outside the combining function or component is more flexible
;; since we have full control over their creation and configuration.

(defn merge-chans
  "Merge channels and returns a single output channel combining the traffic into
  a single linear stream"
  [machine-chan human-chan unwknown-chan]
  (async/merge [machine-chan human-chan unwknown-chan] (async/dropping-buffer 10)))

;; we are now going to feed each upstream channel with the data knowing that each
;; is specialized with only one type of data
(let [hs (filter #(= (:type %) :human) main-characters)
      ms (filter #(= (:type %) :terminator) main-characters)
      as aliens
      merged-chans (merge-chans machine-chan human-chan unknown-chan)]
  ;; specialized input stream
  (async/go
    (doseq [h hs]
      (>! human-chan (merge h {:from :human-chan}))))

  (async/go
    (doseq [m ms]
      (>! machine-chan (merge m {:from :machine-chan}))))

  (async/go
    (doseq [a as]
      (>! unknown-chan (merge a {:from :unknown-chan}))))

  ;; and the last processor will extract from the output channel merged-chans
  (async/go-loop []
    (println "Received :" (<! merged-chans))
    (recur)))

;; > REPL OUTPUT
;; Received : {:model :t1000, :type :terminator, :from :machine-chan}
;; Received : {:name ET, :type :unknown, :from :unknown-chan}
;; Received : {:name Predator, :type :unknown, :from :unknown-chan}
;; Received : {:model :tx, :type :terminator, :from :machine-chan}
;; Received : {:firstname John, :lastname Connor, :type :human, :from :human-chan}
;; Received : {:firstname Sarah, :lastname Connor, :type :human, :from :human-chan}

;; We can see that each item has been enriched through their respective input channels
;; with the additional :from field and that they have been received by the output


;; let's now consider a case where we would like to aggregate those input channels
;; but also allow some of them to be `muted` or `paused` for e.g.
;; The first thing to do is to define our different upstream and output channels.

(def m-human-chan (async/chan (async/dropping-buffer 2)))
(def m-machine-chan (async/chan (async/dropping-buffer 2)))
(def m-unknown-chan (async/chan (async/dropping-buffer 2)))

(def m-out-chan (async/chan (async/dropping-buffer 2)))

;; we will then use `mix` to create that output stream and `admix` to add an input
;; channel to the mix.
(defn mix-chans
  "Takes multiples channels as input and mixes them into one."
  [human-chan machine-chan unknown-chan out-chan]
  (let [mixed-chan (async/mix out-chan)]
    ;; we then add all the input stream channels into the mix
    (async/admix mixed-chan human-chan)
    (async/admix mixed-chan machine-chan)
    (async/admix mixed-chan unknown-chan)
    ;; and finally we return the mixed-chan
    mixed-chan))

;; at this point if we feed those channels with data, the mixed-chan will behave
;; like a normal `merge`d channel seen previously.
(let [hs (filter #(= (:type %) :human) main-characters)
      ms (filter #(= (:type %) :terminator) main-characters)
      as aliens
      mixed-chans (mix-chans m-human-chan m-machine-chan m-unknown-chan m-out-chan)]
  ;; specialized input stream
  (async/go
    (doseq [h hs]
      (>! m-human-chan h)))

  (async/go
    (doseq [m ms]
      (>! m-machine-chan m)))

  (async/go
    (doseq [a as]
      (>! m-unknown-chan a)))

  ;; and we extract what have been pushed into the mixed output channel
  (async/go-loop []
    (println "Mixed in : " (<! m-out-chan))
    (recur)))

;; > REPL OUTPUT
;; Mixed in :  {:lastname Connor, :type :human, :firstname John}
;; Mixed in :  {:name Predator, :type :unknown}
;; Mixed in :  {:name ET, :type :unknown}
;; Mixed in :  {:lastname Connor, :type :human, :firstname Sarah}
;; Mixed in :  {:lastname Reeves, :type :human, :firstname Kyle}
;; Mixed in :  {:type :terminator, :model :t1000}
;; Mixed in :  {:type :terminator, :model :tx}
;; user>

;; All items from the input channels are mixed in and retrieve on the output channel


;; let's now rework it a little bit and see if we can remove one of the input channels
;; from the mix. To do that, we can use the `unmix` function
;;
;; IMPORTANT: we have to reevaluate the channel definitions from earlier
;; (def m-human-chan (async/chan (async/dropping-buffer 2)))
;; (def m-machine-chan (async/chan (async/dropping-buffer 2)))
;; (def m-unknown-chan (async/chan (async/dropping-buffer 2)))
;; (def m-out-chan (async/chan (async/dropping-buffer 2)))

(let [hs (filter #(= (:type %) :human) main-characters)
      ms (filter #(= (:type %) :terminator) main-characters)
      as aliens
      mixed-chans (mix-chans m-human-chan m-machine-chan m-unknown-chan m-out-chan)]
  ;; for the sake of e.g let's remove it here : we want to get rid of the aliens
  ;; channels so that in the final output stream chan we do not have not :unknown type
  ;; items
  (async/unmix mixed-chans m-unknown-chan)
  
  ;; specialized input stream
  (async/go
    (doseq [h hs]
      (>! m-human-chan h)))

  (async/go
    (doseq [m ms]
      (>! m-machine-chan m)))

  (async/go
    (doseq [a as]
      (>! m-unknown-chan a)))

  ;; and we extract what have been pushed into the mixed output channel
  (async/go-loop []
    (println "Mixed in (without unknowns): " (<! m-out-chan))
    (recur)))

;; > REPL OUTPUT (no :unknowns in the final output channel)
;; Mixed in (without unknowns):  {:type :terminator, :model :tx}
;; Mixed in (without unknowns):  {:type :terminator, :model :t1000}
;; Mixed in (without unknowns):  {:type :terminator, :model :tx}
;; Mixed in (without unknowns):  {:lastname Connor, :type :human, :firstname John}
;; Mixed in (without unknowns):  {:lastname Connor, :type :human, :firstname Sarah}


;; finally, let's try to `mute`and `pause` two of the input stream channels.
;; There is a little nuance between those two settings. A paused input stream channels's
;; content will not be consumed. Instead, a muted input stream channel's content will be
;; read but not included in the output stream channel.
;;
;; IMPORTANT: we have to reevaluate the channel definitions from earlier
;; (def m-human-chan (async/chan (async/dropping-buffer 2)))
;; (def m-machine-chan (async/chan (async/dropping-buffer 2)))
;; (def m-unknown-chan (async/chan (async/dropping-buffer 2)))
;; (def m-out-chan (async/chan (async/dropping-buffer 2)))

(let [hs (filter #(= (:type %) :human) main-characters)
      ms (filter #(= (:type %) :terminator) main-characters)
      as aliens
      mixed-chans (mix-chans m-human-chan m-machine-chan m-unknown-chan m-out-chan)]
  ;; for the sake of e.g let's toggle the input channels here : we want to pause the aliens
  ;; channels and mute the machines channel.
  (async/toggle mixed-chans {m-machine-chan {:mute true}
                             m-unknown-chan {:pause true}})
  
  ;; specialized input stream
  (async/go
    (doseq [h hs]
      (>! m-human-chan h)))

  (async/go
    (doseq [m ms]
      (>! m-machine-chan m)))

  (async/go
    (doseq [a as]
      (>! m-unknown-chan a)))

  ;; and we extract what have been pushed into the mixed output channel
  (async/go-loop []
    (println "Mixed in (:muted machines /:paused unknowns): " (<! m-out-chan))
    (recur)))

;; At this point, we should only retrieve item of type :human from the output stream
;; channel

;; > REPL OUTPUT
;; Mixed in (with :muted machines and :paused unknowns):  {:lastname Connor, :type :human, :firstname John}
;; Mixed in (:muted machines /:paused unknowns):  {:lastname Connor, :type :human, :firstname Sarah}
;; Mixed in (with :muted machines and :paused unknowns):  {:lastname Reeves, :type :human, :firstname Kyle}
;; user> 

