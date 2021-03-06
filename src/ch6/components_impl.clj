(ns ch6.components-impl
  (:require [clojure.core.async :as async
             :refer [<! >! go-loop go]]))

;; So far, we have seen how to call components via their api and how we can connect
;; them together. It is now time to see how to implement the functionality behind those
;; apis.
;; While implementing components, we have to consider two things :
;; - that the component has some kind of `internal state` that callers are allowed to alter
;; and change via the exposed fns on the component's api. The component also ensures
;; the proper update of those states. We have seen previously notions related to concurrent
;; access through Software transaction memory and the usage of atoms, refs etc. in ensuring
;; that concurrent thread accessing those shared state do not accidentally corrupt them.

;; - that the component has a `lifecycle` : it can be created and provided with 
;; externally-defined `configuration data` and `dependencies` allowing it to be dynamic and
;; thus reusable in different settings by initializing internal states or connections to
;; external systems. It can also be started and stopped which is an important characteristic
;; for REPL-driven development.


;; GRANULARITY OF STATE
;; Holding the value of the component's internal state requires the use of `stateful` containers
;; as seen with STM. We then have to decide on the granularity of the state.
;; - We can opt to use `refs` if we have some `coordination` to handle i.e if two or more state
;; have to be updated in the same transaction.
;; - We can use `atom` if there are no coordination involved.

;; Let's consider for example an index of the terminator models and an index of missions that
;; we have to manage together.

(defrecord TerminatorMissions [models missions])

(comment

  ;; we can have the following component creation function
  (defn make-terminator-missions
    [models missions]
    (map->TerminatorMissions {:models (atom [])
                              :missions (atom [])}))

  ;; those are independent states that need to change separately. There is no coordination
  ;; involved...
  )


;; if on the other side the two states have to be modified atomically, we would implement it
;; like the following

(comment

  (defn make-terminator-missions
    [models missions]
    (map->TerminatorMissions {:models (ref {})
                              :missions (ref {})}))

  ;; the only issue with refs is that we would have to wrap all our interactions with the
  ;; the internal states within a transaction (dosync ...)
  )

;; an elegant solution would be to increase the granularity of the state container to 
;; encompass both :models and :missions

(comment

  (defn make-terminator-missions
    [models missions]
    (map->TerminatorMissions (atom {:models {}
                                    :missions {}})))

  ;; we would then have only one global state container for the component which is
  ;; much simpler to manage. Coarser-grained state use is often possible assuming that
  ;; the update functions are small.
  )


;; CONFIGURATION
;; While implementing a component, it will need to hold information for startup and
;; ongoing use. Typically, we can group those in the following 3 categories :
;;  - configuration
;;  - dependencies
;;  - runtime state

;; Configuration data is externally-provided to the component (by the `assembly`). It is
;; passed to the component at `creation` time and can be used when the component has
;; to be started or later. E.g : database connection, middleware systems conf etc. can all
;; be part of the configuration.

;; when creating the component, we can then pass those informations like so :
(comment

  (defn new-component [db-url username password])

  ;; the issue with this is that configurations, most of the time, evolve over time. So
  ;; at some point, it will consist of more than those 3 attributes : e.g an additional
  ;; messaging system url might be added ...
  ;; if we modify the new-component function by adding this extra parameter, every other
  ;; caller component (most probably assembly though here) would then break and need to
  ;; be updated with this extra param
  )

;; a much better solution is to actually bundle the configuration in a map or record
;; like so :

(comment

  (defn new-component [{:keys [db-url username password message-system-url]}])

  ;; if needed, we can evolve the configuration without breaking the code since the
  ;; whole configuration is packed within this sole map parameter
  )

;; Sometimes, component might need to rely on another component to accomplish some tasks.
;; Thus there is a dependency among them.

(comment

  (defn new-skynet [config robot-builder])

  ;; instead of providing the component as a `direct` dependency, the best practice is
  ;; to `decouple` the components and use a channel which would then act as the
  ;; dependency of the component. This way, we have a loose coupling between the components
  ;; A channel offers more flexibility on how to connect this component with other components
  ;; as we have seen with what core.async is capable of...
  )

;; Because of the need to store those information (configuration, internal state
;; dependencies etc.) internally, `records` are the best choice to implement component
;; because it allows to provide the component's `behaviour` as protocol implementations
;; on those records.


;; LIFECYCLE
;; Components have lifecycle : they are first `created` and then can be `started` and/or `stopped`.
;; Let's say for the sake of example that we have a component that is responsible for
;; programming the terminators based on a set of rules.

(comment

  (defrecord skynet-bot-programming-component
      [config ;; configuration
       ch-in ;; for receiving information from other components
       ch-out ;; output channel for sending information to other components
       rules ;; the set of rules to apply for each model of robot
       active ;; a flag for when the component is alive and ready to work
       ])

  ;; Most of the time, components need to do some initial setup on creation.
  ;; Having a convenience function like the following helps with that step.
  (defn make-bot-programming-component
    [config ch-in ch-out rules]
    (map->skynet-bot-programming-component {:config config
                                            :ch-in ch-in
                                            :ch-out ch-out
                                            :rules (atom rules)
                                            :active (atom false)})) ;; by default not active

  ;; We then define the function for starting the component. It consists in establishing a
  ;; go-block which will live throughout the life of the component and will attempt to take
  ;; values from the input channel and do some processing before pushing the result to the
  ;; output channel.
  (defn start
    [{:keys [ch-in ch-out rules active] :as skynet}]
    (when (not @active)
      (println "Starting the *component*")
      (reset! active true)
          ;; and then we start a lightweight process which will handle the
          ;; processing of any incoming message received from the ch-in
      (go-loop []
        (let [request (<! ch-in)
              _ (println "[>> comp] Received : " request)
              model (-> request :model)
              response (get @rules model :not-found)
              _ (println "[|*| comp] Processing : " response)
              processed-response (-> response name clojure.string/upper-case)]
          (println "[comp >>] Sent out  : " processed-response)
          (>! ch-out processed-response)
          (when @active (recur))))) ;; here we flag the component as active
    skynet)

  ;; finally, we can implement the function for stopping the component
  (defn stop
    [{:keys [ch-out active] :as skynet}] ;; we are only interested in closing ch-out and resetting active to false
    (when @active
      (println "Stopping the component")
      (reset! active false)  ;; set the component as non-active
      (async/close! ch-out)) ;; stop pushing responses
    skynet)

  ;; our input to be sent to the component for processing
  (def input [{:model :tx} {:model :t500}])

  ;; our set of configuration
  (def config {})
  
  ;; input and output channels for the component
  (def ch-in (async/chan (async/dropping-buffer 2)))
  (def ch-out (async/chan (async/dropping-buffer 2)))

  ;; our set of rules
  (def rules {:t1000 :init-t1000
              :tx :init-tx
              :t500 :initiate})

  ;; we then create our component ... this is just creation giving us a `dead` component 
  ;; waiting to be started!!!
  (def c (make-bot-programming-component config ch-in ch-out rules))

  ;; eval-ing the component allow us to check that indeed the component is not active yet!!
  ;; :active #<Atom@35d574f5: false> as in the following
  c
  ;; > REPL OUTPUT
  ;; user> *1
  ;; {:config {},
  ;;  :ch-in
  ;;  #object[clojure.core.async.impl.channels.ManyToManyChannel 0x2e145c49 "clojure.core.async.impl.channels.ManyToManyChannel@2e145c49"],
  ;;  :ch-out
  ;;  #object[clojure.core.async.impl.channels.ManyToManyChannel 0x63d57182 "clojure.core.async.impl.channels.ManyToManyChannel@63d57182"],
  ;;  :rules
  ;;  #<Atom@7adcd5e4: 
  ;;  {:t1000 :init-t1000, :tx :init-tx, :t500 :initiate}>,
  ;;  :active #<Atom@35d574f5: false>}

  ;; we then start the component via the function defined previously and we can see from the
  ;; evaluation of c that the component is now active
  ;; :active #<Atom@35d574f5: true> as in the following
  (start c)
  ;; > REPL OUTPUT
  ;; Starting the component
  ;; {:config {},
  ;;  :ch-in
  ;;  #object[clojure.core.async.impl.channels.ManyToManyChannel 0x2e145c49 "clojure.core.async.impl.channels.ManyToManyChannel@2e145c49"],
  ;;  :ch-out
  ;;  #object[clojure.core.async.impl.channels.ManyToManyChannel 0x63d57182 "clojure.core.async.impl.channels.ManyToManyChannel@63d57182"],
  ;;  :rules
  ;;  #<Atom@7adcd5e4: 
  ;;  {:t1000 :init-t1000, :tx :init-tx, :t500 :initiate}>,
  ;;  :active #<Atom@35d574f5: true>}

  ;; if we try to eval the starting process at this point, nothing happens ... it is already
  ;; started
  (start c)

  ;; if we stop it now ...
  (stop c)
  ;; > REPL OUTPUT
  ;; Stopping the component
  ;; user>

  ;; and eval-ing the component should display that it is indeed inactive at this point
  ;; :active #<Atom@35d574f5: false> as in the following
  c
  ;; user> *1
  ;; {:config {},
  ;;  :ch-in
  ;;  #object[clojure.core.async.impl.channels.ManyToManyChannel 0x2e145c49 "clojure.core.async.impl.channels.ManyToManyChannel@2e145c49"],
  ;;  :ch-out
  ;;  #object[clojure.core.async.impl.channels.ManyToManyChannel 0x63d57182 "clojure.core.async.impl.channels.ManyToManyChannel@63d57182"],
  ;;  :rules
  ;;  #<Atom@7adcd5e4: 
  ;;  {:t1000 :init-t1000, :tx :init-tx, :t500 :initiate}>,
  ;;  :active #<Atom@35d574f5: false>}


  ;; let's start it back up again...
  (start c)
  ;; > REPL OUTPUT
  ;; Starting the component
  
  ;; let's now set up a lightweight process which will connect to the component output channel,
  ;; extract and display messages received on this channel
  (let [{:keys [ch-out]} c]
    (println "Starting the consuming component...")
    ;; our lightweight process extracting from the component's ch-out...
    (go-loop []
      (if-let [v (<! ch-out)]
        (println "[>> sink consumer] : " v))
      (recur)))

  ;; we can now feed the component with the input we defined earlier and expect the lightweight
  ;; process to display something on the console/repl
  (let [{:keys [ch-in]} c]
    (go
      (doseq [i input]
        (println "Feeding comp >> " i)
        (>! ch-in i))))

  ;; > REPL OUTPUT
  ;; Starting the *component*
  ;; Starting the consuming component...
  ;; Feeding comp >>  {:model :tx}
  ;; Feeding comp >>  {:model :t500}
  ;; [>> comp] Received :  {:model :tx}
  ;; [|*| comp] Processing :  :init-tx
  ;; [comp >>] Sent out  :  INIT-TX
  ;; [>> comp] Received :  {:model :t500}
  ;; [ >> sink consumer] :  INIT-TX
  ;; [|*| comp] Processing :  :initiate
  ;; [comp >>] Sent out  :  INITIATE
  ;; [ >> sink consumer] :  INITIATE
  )
