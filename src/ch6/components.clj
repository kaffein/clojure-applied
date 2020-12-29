(ns ch6.components)

;; There are two ways of organizing code in Clojure :
;; - namespaces
;; - components

;; `namespaces` allows the grouping of functions, protocols, vars etc together.
;; They are language constructs for organizing code. Everything has to belong to
;; a namespace.
;; Most of the time, we introduce `logical` grouping within namespace though.
;; Logically grouping functions, vars or other language contructs that are related
;; helps in maintaining and keeping the codebase organized as the application grows.

;; `components` on the other hand are high-level constructs related to the domain
;; problem. It is a way of organizing functions, protocols, vars etc such that
;; they form a cohesive unit responsible for solving one aspect of the problem the
;; system tries to solve.
;; That characteristic also allows the component to be reused.
;; A component has 3 major characteristics :
;; - a public api : which acts as the `contract` defining the set of `services` or `capabilities`
;; the component can offer to any other external component. Typically, those are public
;; functions that any other external components may invoke.
;; - an internal implementation : which is the concrete set of instructions corresponding
;; to each public api functions and which defines `how things are done` concretely.
;; - an internal state : which can use concurrency internally to process data in parallel.


;; DESIGNING COMPONENT APIS
;; The first step in defining a component is to figure out `what aspect` of the problem
;; at hand is the component going to solve. Having done this inventory work, we can then
;; deduce the set of public functions of the component api needed to accomplish the task and
;; that the outside consumers will use.

;; Interacting with components
;; There are two ways to interact with components :
;; - invoking functions (those exposed by the component's public api)
;; - passing messages on a queue or channel

;; Manipulating components with functions
;; We can directly call the functions exposed by the component's public api. Whenever possible,
;; components should expose immutable data. Immutability will allow the consumer to manipulate
;; transform, query the data returned by the component without affecting the component's data.

;; Let's get back once again to our previous example.
;; Suppose Skynet has a central component called genisys responsible for Terminator management.
;; The roles associated with this component (its api) are :
;; - adding a new terminator model
;; - replace a terminator model
;; - delete a terminator model
;; - find a terminator model based on a criteria
;; - instructs a terminator model to execute a set of orders for init

;; We can then assume that the following are the functions needed by genisys for those functions
;; read interface
(defn get-models [skynet])
(defn find-model [skynet model])

;; update interface
(defn add-model [skynet model])
(defn replace-model [skynet old-model new-model])
(defn delete-model [skynet model])

;; processing interface
(defn init-command [skynet model cmds])

;; we could then use those commands like so
(comment

  (defn new-skynet
    "Creates skynet"
    []
    {})
  
  (let [skynet (new-skynet)]
    (add-model skynet :t800)
    (add-model skynet :t1000)
    (add-model skynet :t850)
    (replace-model skynet :t800 :tx)
    (delete-model skynet :t1000)
    (get-models skynet)))

;; ############################################################################
;; When we get a deeper look at the previous code though, we can see that there
;; is a smaller set of functions that can support the whole api
(comment

  ;; retrieves all terminator models from skynet
  (defn get-models [skynet])

  ;; single transformation fn for updating the set of available terminator models
  ;; in skynet including : adding a model, replacing a model and removing a model
  (defn transform-models [skynet update-fn])

  ;; init-command
  (defn init-command [skynet model cmds])

  )

;; Most APIs are layered this way : a handful key base functions that are composable
;; and used to implement larger set of functions which are easy to use and will be
;; provided to other external components for consumption.
;; Clojure `protocols` are the mechanism used to `capture` those key base functions.
;; As a reminder, protocols are like Java interfaces, providing an abstraction layer
;; defining what a component implementing this interface will provide in terms of service
;; to other components using it. This is a `contract` that the component will provide to
;; any other external components using it. Being able to capture it within this thin
;; layer of abstraction also allows for `reuse`. In fact, any component that is capable of
;; `honouring` this contract can be `plugged-in` in lieu of another one without the
;; consumer component having to be modified. As long as they `implement` the same protocol
;; they can be swapped.

;; applying this to our example would give :

(comment

  ;; We call it a SPI protocol : service provider interface
  (defprotocol ISkynet
    (get-models [skynet])
    (transform-models [skynet update-fn])
    (init-command [skynet model cmds]))

  ;; we might have some private util functions within the same namespace

  ;; but most importantly, we have the larger set of functions that are `expressed` and
  ;; layered over that abstraction and which constitutes the `api`.

  (defn find-model
    [skynet model]
    (filter (= model (:model %)) (get-models skynet))) ; here we use `get-models` from the SPI

  (defn add-model
    [skynet model]
    (transform-models skynet #(conj % model))) ; here we use `transform-models` from the SPI

  (defn remove-model
    [skynet model]
    (transform-models skynet #(disj % model))) ; here we use `transform-models` from the SPI

  (defn replace-model
    [skynet old-model new-model]
    (transform-models skynet #(-> % (disj old-model) (conj new-model)))) ; here we use `transform-models` from the SPI

  ;; In all the previous sample calls, we can give an alternative implementation to the underlying
  ;; protocol functions by providing another reification of the protocol
)


;;############################################################################
;; So far we made synchronous call to the functions defined by the api. Sometimes
;; though, those calls might take some time to return the response. So instead of
;; blocking, we can make the call `asynchrounous`, allowing it to be offloaded from
;; the main thread and returning the result of the processing later when it is ready.

;; We have two (2) main ways to just do that :
;; - returning a `future` or a `promise` where the result will be delivered, from the function.
;; The consumer code then has the choice of when it wants to block while waiting for
;; the response.
;; - using a `callback` that we will on illustrate since this a practice that we would
;; like to avoid as much as possible.

(comment

  ;; let's suppose for a moment that the init-command here returns a future
  (let [response-future (init-command skynet :t1000 ["init" "start" "launch"])]
    ... ;; we might eval some other forms here
    @response-future) ;; the consumer code then derefs the future here

  ;; let's suppose for a second that the init-command here returns a promise
  (let [response-promise (init-command skynet :t500 ["init" "start" "transform"])]
    ... ;; same thing, we night have some other evaluation here
    @response-promise) ;; then the consumer code decides when/where to block to get the result
  )
